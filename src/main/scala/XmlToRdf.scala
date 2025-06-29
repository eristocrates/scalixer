import cats.effect.{IO, IOApp}
import fs2.Stream
import fs2.data.xml._
import fs2.data.xml.XmlEvent._
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Paths, Files, Path}
import scala.util.hashing.MurmurHash3
import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.jdk.CollectionConverters._

object XmlToRdf extends IOApp.Simple {

  // TODO add namespaces and their iris discovered during streaming
  private val prefixMap: Map[String, String] = ListMap(
    "rdf"  -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
    "owl"  -> "http://www.w3.org/2002/07/owl#",
    "ex"   -> "http://example.org/",
    "prov" -> "http://www.w3.org/ns/prov#",
    "xsd" -> "http://www.w3.org/2001/XMLSchema#"

  )

  val rdfHeader = {
    val prefixes = prefixMap.map { case (p, iri) => s"  xmlns:$p=\"$iri\"" }.mkString("\n")
    s"""<?xml version="1.0"?>
<rdf:RDF\n$prefixes>"""
  }

  val rdfFooter = "\n</rdf:RDF>"

  private def normalizeLiteral(value: String): String =
    value.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "")

  private def pascalCase(value: String): String =
    value
      .split("[^\\p{IsAlphabetic}\\p{IsDigit}]+")
      .filter(_.nonEmpty)
      .map(word => word.head.toUpper + word.tail)
      .mkString

  private def pascalSnakeCase(value: String): String =
    value
      .split("[^\\p{IsAlphabetic}\\p{IsDigit}]+")
      .filter(_.nonEmpty)
      .map(word => word.head.toUpper + word.tail.toLowerCase)
      .mkString("_")

  private def expandPrefix(name: String): String =
    name.split(":", 2) match
      case Array(prefix, local) if prefixMap.contains(prefix) => prefixMap(prefix) + local
      case _                                                  => name

  // TODO use actual prefx and fall back on base if none exist
  private def createHasProperty(tag: String): String =
    s"ex:has${pascalCase(tag)}"

  private def createSyntacticIRI(prefix: Option[String], tag: String, stack: List[(String, String)], count: Int): String =
    val base = (tag :: stack.map(_._2)).mkString("/") + s"$count"
    val pfx = prefix.getOrElse("ex")
    expandPrefix(s"$pfx:${tag}_${count}")
    // kexpandPrefix(s"ex:${tag}_${MurmurHash3.stringHash(base)}")

  private def createSemanticIRI(value: String): String =
    expandPrefix(s"ex:${pascalSnakeCase(value)}")

  private def syntacticClassIRI(prefix: Option[String], tag: String): String =
    val pfx = prefix.getOrElse("ex")
    expandPrefix(s"$pfx:${pascalCase(tag)}_Tag")

  private def semanticClassIRI(prefix: Option[String], tag: String): String =
    val pfx = prefix.getOrElse("ex")
    expandPrefix(s"$pfx:${pascalCase(tag)}")

  private def escapeXml(s: String): String =
    s.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")

  enum TagRole:
    case EntityTag,
         PropertyTag,
         ContainerTag,
         TypenameTag,
         ReferenceTag,
         AnnotationTag,
         CollectionTag

  enum StringRole:
    case LabelString,
         LiteralValueString,
         IdentifierString,
         ReferenceString,
         ClassValueString,
         EmptyString,
         MixedContentString

  lazy val tagRoleMap: Map[String, Set[String]] = loadRoleMap(tagRoleDir)

  def tagRole(tag: String): Option[String] =
    tagRoleMap.collectFirst { case (role, tags) if tags.contains(tag) => role }

  lazy val stringRoleMap: Map[String, Set[String]] = loadRoleMap(stringRoleDir)

  def stringRole(tag: String): Option[String] =
    stringRoleMap.collectFirst { case (role, tags) if tags.contains(tag) => role }

  def inferLiteralType(text: String): String = text.trim match { 
    case s if s.matches("""^-?\d+\.\d+$""") => "xsd:decimal"
    case s if s.matches("""^-?\d+$""")      => "xsd:integer"
    case s if s.matches("""\d{4}-\d{2}-\d{2}""") => "xsd:date"
    case _                                  => "xsd:string"
  }

  val rolesDir = Paths.get("roles")
  val tagRoleDir = rolesDir.resolve("TagRoles")
  val stringRoleDir = rolesDir.resolve("StringRoles")

  Files.createDirectories(tagRoleDir)
  Files.createDirectories(stringRoleDir)

  // Role files are user supplied. Presence of any non-empty file enables
  // semantic RDF/XML emission.
  private def hasRoleFiles(dir: Path): Boolean =
    Files.exists(dir) &&
      {
        val stream = Files.list(dir)
        try stream.iterator().asScala.exists(p => Files.isRegularFile(p) && Files.size(p) > 0)
        finally stream.close()
      }

  lazy val semanticEnabled: Boolean =
    hasRoleFiles(tagRoleDir) || hasRoleFiles(stringRoleDir)

  def liftEvent(
      lang: Option[String],
      path: java.nio.file.Path
  ): XmlEvent => Stream[IO, String] = {
    val tagSet = mutable.Set[String]()
    val tagToStrings = mutable.Map[String, mutable.Set[String]]().withDefault(_ => mutable.Set())

    var stack: List[(String, String)]    = Nil
    var emittedClasses: Set[String]      = Set.empty
    var emittedSemantic: Set[String]     = Set.empty
    // event counter used as approximate source line number
    var lineCounter: Int                 = 0
    val liftedBy: String                 = java.util.UUID.randomUUID().toString
    val tagCounter: scala.collection.mutable.Map[String, Int] =
      scala.collection.mutable.Map.empty.withDefaultValue(0)
    val fileName = path.getFileName.toString
    val fileExtension = {
      val idx = fileName.lastIndexOf('.')
      if (idx >= 0 && idx < fileName.length - 1) fileName.substring(idx + 1)
      else "unknown"
    }
    val fileBaseName = {
      val idx = fileName.lastIndexOf('.')
      if (idx > 0) fileName.substring(0, idx) else fileName
    }

    val filePathStr = path.toAbsolutePath.toString
    val fileUri = "file:///" + filePathStr.replace("\\", "/")
      
    val documentProvenance = List(
      s"\n<rdf:Description rdf:about=\"${expandPrefix("ex:Document")}\">",
      s"  <rdf:type rdf:resource=\"${expandPrefix("ex:XmlDocument")}\"/>",
      s"  <ex:filePath rdf:datatype=\"${expandPrefix("xsd:string")}\" >${escapeXml(fileUri)}</ex:filePath>",
      s"  <ex:fileBaseName rdf:datatype=\"${expandPrefix("xsd:string")}\">${escapeXml(fileBaseName)}</ex:fileBaseName>",
      s"  <ex:fileExtension rdf:datatype=\"${expandPrefix("xsd:string")}\">${escapeXml(fileExtension)}</ex:fileExtension>",
      s"  <ex:fileName rdf:datatype=\"${expandPrefix("xsd:string")}\">${escapeXml(fileName)}</ex:fileName>",
      s"</rdf:Description>"
    )

    {
      case _: XmlEvent.StartDocument.type =>
        Stream.emits(documentProvenance)

      case XmlDoctype(name, externalId, internalSubsetOpt) =>
        val lines = List(
          s"\n<rdf:Description rdf:about=\"ex:Document\">",
          s"  <rdf:type rdf:resource=\"${expandPrefix("owl:Ontology")}\"/>",
          s"  <rdfs:label>${escapeXml(name)}</rdfs:label>",
          s"  <ex:externalId>${escapeXml(externalId)}</ex:externalId>"
        ) ++ internalSubsetOpt.map(sub => s"  <ex:internalSubset>${escapeXml(sub)}</ex:internalSubset>") ++
          List("</rdf:Description>")

        Stream.emits(lines)

      case StartTag(qn, attrs, _) =>
        val tag = qn.local
        val prefix = qn.prefix
        
        tagSet += tag
        tagCounter(tag) += 1

        val synClassIRI   = syntacticClassIRI(prefix, tag)
        val semClassIRI   = semanticClassIRI(prefix, tag)

        val classBlocks =
          if !emittedClasses.contains(tag) then
            emittedClasses += tag
            val base = List(
              s"<rdf:Description rdf:about=\"$synClassIRI\">\n  <rdf:type rdf:resource=\"${expandPrefix("owl:Class")}\"/>\n</rdf:Description>"
            )
            val sem =
              if semanticEnabled then
                List(
                  s"<rdf:Description rdf:about=\"$semClassIRI\">\n  <rdf:type rdf:resource=\"${expandPrefix("owl:Class")}\"/>\n</rdf:Description>"
                )
              else Nil
            base ++ sem
          else Nil

        val subjectIRI = createSyntacticIRI(prefix, tag, stack, tagCounter(tag))

        val parentBlock = stack.headOption.map { case (parentIRI, _) =>
          s"<rdf:Description rdf:about=\"$parentIRI\">\n  <rdfs:member rdf:resource=\"$subjectIRI\"/>\n</rdf:Description>"
        }

        val attrLines = attrs.collect {
          case Attr(QName(_, "lang"), _) => None
          case Attr(name, value) =>
            val attrVal = value.collect { case XmlString(s, _) => s }.mkString
            val prop    = createHasProperty(name.local)
            val dt      = expandPrefix(inferLiteralType(attrVal))
            Some(s"  <$prop rdf:datatype=\"$dt\">${escapeXml(attrVal)}</$prop>")
        }.flatten

        lineCounter += 1
        val xmlPath   = "/" + (stack.map(_._2).reverse :+ tag).mkString("/")
        val provenanceLines = List(
          s"  <prov:wasDerivedFrom rdf:resource=\"$fileUri\"/>",
          s"  <ex:sourceLine rdf:datatype=\"${expandPrefix("xsd:integer")}\">$lineCounter</ex:sourceLine>",
          s"  <ex:sourceXmlPath rdf:datatype=\"${expandPrefix("xsd:string")}\">${escapeXml(xmlPath)}</ex:sourceXmlPath>",
          s"  <ex:liftedBy rdf:datatype=\"${expandPrefix("xsd:string")}\">$liftedBy</ex:liftedBy>"
        )

        val subjectBlock =
          (List(s"<rdf:Description rdf:about=\"$subjectIRI\">", s"  <rdf:type rdf:resource=\"$synClassIRI\"/>") ++
            provenanceLines ++
            attrLines ++
            List("</rdf:Description>")).mkString("\n")

        stack = (subjectIRI, tag) :: stack

        Stream.emits(classBlocks ++ parentBlock.toList :+ subjectBlock)

      case XmlString(text, _) if text.trim.nonEmpty =>
        stack.headOption match
          case Some((_, tag)) =>
            tagToStrings(tag) += text.trim
            if semanticEnabled then
              val valueIRI  = createSemanticIRI(text.trim)
              val classIRI  = semanticClassIRI(None, tag)
              val hasProp   = createHasProperty(tag)
              val parentIRI = stack.drop(1).headOption.map(_._1)

              val parentBlock = parentIRI.map { p =>
                s"<rdf:Description rdf:about=\"$p\">\n  <$hasProp rdf:resource=\"$valueIRI\"/>\n</rdf:Description>"
              }

              val valueBlock =
                if !emittedSemantic.contains(valueIRI) then
                  emittedSemantic += valueIRI
                  lineCounter += 1
                  val xmlPathValue = "/" + stack.map(_._2).reverse.mkString("/") + "/text()"
                  val valueProvenance = List(
                    s"  <prov:wasDerivedFrom rdf:resource=\"$fileUri\"/>",
                    s"  <ex:sourceLine rdf:datatype=\"${expandPrefix("xsd:integer")}\">$lineCounter</ex:sourceLine>",
                    s"  <ex:sourceXmlPath rdf:datatype=\"${expandPrefix("xsd:string")}\">${escapeXml(xmlPathValue)}</ex:sourceXmlPath>",
                    s"  <ex:liftedBy rdf:datatype=\"${expandPrefix("xsd:string")}\">$liftedBy</ex:liftedBy>"
                  )
                  Some(
                    (
                      List(s"<rdf:Description rdf:about=\"$valueIRI\">", s"  <rdf:type rdf:resource=\"$classIRI\"/>", s"  <rdfs:label xml:lang=\"${lang.getOrElse("en")}\">${escapeXml(text.trim)}</rdfs:label") ++
                        valueProvenance ++
                        List("</rdf:Description>")
                    ).mkString("\n")
                  )
                else None

              Stream.emits(parentBlock.toList ++ valueBlock.toList)
            else Stream.empty
          case None => Stream.empty

      case EndTag(_) =>
        stack = stack.drop(1)
        Stream.empty

      case _ => Stream.empty
    }
  }

  def run: IO[Unit] = {
    val in: InputStream = getClass.getResourceAsStream("/example.xml")
    if (in == null) IO.raiseError(new IllegalArgumentException("Missing example.xml"))
    else {
      val xmlEvents =
        fs2.io.readInputStream(IO.pure(in), 4096)
          .through(fs2.text.utf8.decode)
          .through(events[IO, String]())

      val rdfStream = xmlEvents.flatMap(liftEvent(Some("en"), Paths.get("src/main/resources/example.xml")))

      val output = Stream.emit(rdfHeader) ++
        rdfStream.intersperse("\n") ++
        Stream.emit(rdfFooter)

      output
        .through(fs2.text.utf8.encode)
        .through(fs2.io.file.Files[IO].writeAll(Paths.get("example.rdf")))
        .compile
        .drain
    }
  }
  def sanitizeForFilename(tag: String): String =
    tag.trim
      .replaceAll("""[\\/:*?"<>|]""", "_") // Windows forbidden characters
      .replaceAll("\\s+", "_")             // Replace whitespace with underscores
      
  def loadRoleMap(roleDir: Path): Map[String, Set[String]] = {
    if (!Files.exists(roleDir)) Files.createDirectories(roleDir)
    Files.list(roleDir).iterator().asScala
      .filter(p => Files.isRegularFile(p) && p.toString.endsWith(".txt"))
      .map { path =>
        val roleName = path.getFileName.toString.stripSuffix(".txt")
        val tags = Files.readAllLines(path, StandardCharsets.UTF_8).asScala.map(_.trim).filter(_.nonEmpty).toSet
        roleName -> tags
      }.toMap
  }

  def runInferAndLexicon: IO[Unit] = {
    val in: InputStream = getClass.getResourceAsStream("/example.xml")
    if (in == null) IO.raiseError(new IllegalArgumentException("Missing example.xml"))
    else {
      val tagSet = mutable.Set[String]()
      val tagToStrings = mutable.Map[String, mutable.Set[String]]().withDefault(_ => mutable.Set())
      var stack: List[String] = Nil

      val xmlEvents =
        fs2.io.readInputStream(IO.pure(in), 4096)
          .through(fs2.text.utf8.decode)
          .through(events[IO, String]())

      val process = xmlEvents.evalMap {
        case StartTag(qn, attrs, _) =>
          val tag = qn.local
          tagSet += tag
          stack = tag :: stack

        for (attr <- attrs) {
          val attrTag = s"@${attr.name.local}"
          tagSet += attrTag
          val strings = tagToStrings.getOrElseUpdate(attrTag, mutable.Set())
          strings += attr.value.collect { case XmlString(text, _) => text }.mkString.trim
        }

          IO.unit
        case EndTag(_) =>
          stack = stack.drop(1)
          IO.unit

        case XmlString(text, _) if text.trim.nonEmpty =>
          val str = text.trim
          stack.headOption match {
            case Some(tag) =>
              tagSet += tag
              val strings = tagToStrings.getOrElseUpdate(tag, mutable.Set())
              strings += str
            case None => // Ignore
          }
          IO.unit

        case _ => IO.unit
      }

      for {
        _ <- process.compile.drain
        _ <- IO {
          val tagDir = Paths.get("tags")
          Files.createDirectories(tagDir)
          Files.write(tagDir.resolve("tags.txt"), tagSet.toList.sorted.asJava)

          // Write each tag's value list
          for (tag <- tagSet) {
            val sanitizedTag = sanitizeForFilename(tag)
            val lines = tagToStrings.getOrElse(tag, mutable.Set.empty).toList.sorted
            Files.write(tagDir.resolve(s"$sanitizedTag.txt"), lines.asJava)
          }

          // --- Datatype inference helper ---
          def inferDatatype(values: Iterable[String]): String = {
            def allMatch(regex: String): Boolean =
              values.nonEmpty && values.forall(_.matches(regex))

            if (allMatch("""true|false""")) "xsd:boolean"
            else if (allMatch("""[+-]?\d+""")) "xsd:decimal"
            else if (allMatch("""[+-]?(\d*\.\d+|\d+\.\d*)([eE][+-]?\d+)?""")) "xsd:double"
            else if (allMatch("""\d{4}-\d{2}-\d{2}""")) "xsd:date"
            else if (allMatch("""\d{4}-\d{2}""")) "xsd:gYearMonth"
            else if (allMatch("""\d{4}""")) "xsd:gYear"
            else if (allMatch("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}""")) "xsd:dateTime"
            else "xsd:string"
          }

          // --- Emit summary.tsv ---
          val summaryHeader = "tag\tcount\tfilename\tinferredDatatype\ttagRole\tstringRole\n"
          val summaryLines = tagSet.toList.sorted.map { tag =>
            val strings = tagToStrings.getOrElse(tag, mutable.Set.empty)
            val count = strings.size
            val sanitizedTag = sanitizeForFilename(tag)
            val filename = s"tags/$sanitizedTag.txt"
            val inferredDatatype = inferDatatype(strings)
            val tagRole = "" // editable by user
            val stringRole = if (inferredDatatype != "xsd:string") "LiteralValueString" else ""

            s"$tag\t$count\t$filename\t$inferredDatatype\t$tagRole\t$stringRole"
          }
          // Write summary.tsv
          val summaryPath = tagDir.resolve("summary.tsv")
          Files.write(summaryPath, (summaryHeader + summaryLines.mkString("\n")).getBytes(StandardCharsets.UTF_8))

          // --- Emit summary.csvw metadata ---
          val summaryMetaPath = tagDir.resolve("summary.csv-metadata.json")
          val csvwJson =
            s"""
              {
              "@context": "http://www.w3.org/ns/csvw",
              "url": "summary.tsv",
              "tableSchema": {
                "columns": [
                  {
                    "name": "tag",
                    "titles": "Tag",
                    "datatype": "string",
                    "description": "Tag found in XML file"
                  },
                  {
                    "name": "count",
                    "titles": "Count",
                    "datatype": "integer",
                    "description": "Number of unique strings for this tag"
                  },
                  {
                    "name": "filename",
                    "titles": "Filename",
                    "datatype": "string",
                    "description": "Output path of .txt file containing tag strings"
                  },
                  {
                    "name": "inferredDatatype",
                    "titles": "Inferred Datatype",
                    "datatype": "string",
                    "description": "XSD primitive type inferred from the values"
                  },
                  {
                    "name": "tagRole",
                    "titles": "Tag Role",
                    "datatype": "string",
                    "description": "User-defined semantic role of the tag"
                  },
                  {
                    "name": "stringRole",
                    "titles": "String Role",
                    "datatype": "string",
                    "description": "User-defined semantic role of the string; defaults to 'LiteralValueString' for non-xsd:string types"
                  }
                ],
                "primaryKey": "tag",
                "description": "Summary of tags and inferred datatypes from XML, for staging user-defined semantic roles"
              }
            }
            """.stripMargin.trim

          Files.write(summaryMetaPath, csvwJson.getBytes(java.nio.charset.StandardCharsets.UTF_8))
        }
      } yield ()
    }
  }

}

