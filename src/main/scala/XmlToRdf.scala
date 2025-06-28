import cats.effect.{IO, IOApp}
import fs2.Stream
import fs2.data.xml._
import fs2.data.xml.XmlEvent._
import java.io.InputStream
import java.nio.file.{Paths, Files}
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
        WrapperTag,
        ReferenceTag,
        AnnotationTag

  enum StringRole:
    case LabelString,
        LiteralValueString,
        IdentifierString,
        ReferenceString,
        ClassValueString,
        EmptyString,
        MixedContentString

  def inferLiteralType(text: String): String = text.trim match { 
    case s if s.matches("""^-?\d+\.\d+$""") => "xsd:decimal"
    case s if s.matches("""^-?\d+$""")      => "xsd:integer"
    case s if s.matches("""\d{4}-\d{2}-\d{2}""") => "xsd:date"
    case _                                  => "xsd:string"
  }
  def liftEvent(
      lang: Option[String],
      path: java.nio.file.Path
  ): XmlEvent => Stream[IO, String] = {
    val tagSet = mutable.Set[String]() 
    val tagToStrings = mutable.Map[String, mutable.Set[String]]().withDefault(_ => mutable.Set())

    var stack: List[(String, String)]    = Nil
    var emittedClasses: Set[String]      = Set.empty
    var emittedSemantic: Set[String]     = Set.empty
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
          if (!emittedClasses.contains(tag)) then
            emittedClasses += tag
            List(
              s"<rdf:Description rdf:about=\"$synClassIRI\">\n  <rdf:type rdf:resource=\"${expandPrefix("owl:Class")}\"/>\n</rdf:Description>",
              s"<rdf:Description rdf:about=\"$semClassIRI\">\n  <rdf:type rdf:resource=\"${expandPrefix("owl:Class")}\"/>\n</rdf:Description>"
            )
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
            Some(s"  <$prop>${escapeXml(attrVal)}</$prop>")
        }.flatten

        val subjectBlock =
          (List(s"<rdf:Description rdf:about=\"$subjectIRI\">", s"  <rdf:type rdf:resource=\"$synClassIRI\"/>") ++
            attrLines ++
            List("</rdf:Description>")).mkString("\n")

        stack = (subjectIRI, tag) :: stack

        Stream.emits(classBlocks ++ parentBlock.toList :+ subjectBlock)

      case XmlString(text, _) if text.trim.nonEmpty =>
        stack.headOption match
          case Some((_, tag)) =>
            val valueIRI  = createSemanticIRI(text.trim)
            val classIRI  = semanticClassIRI(_, tag)
            val hasProp   = createHasProperty(tag)
            val parentIRI = stack.drop(1).headOption.map(_._1)
            
            tagToStrings(tag) += text.trim

            val parentBlock = parentIRI.map { p =>
              s"<rdf:Description rdf:about=\"$p\">\n  <$hasProp rdf:resource=\"$valueIRI\"/>\n</rdf:Description>"
            }

            val valueBlock =
              if !emittedSemantic.contains(valueIRI) then
                emittedSemantic += valueIRI
                Some(
                  s"<rdf:Description rdf:about=\"$valueIRI\">\n  <rdf:type rdf:resource=\"$classIRI\"/>\n  <rdfs:label xml:lang=\"${lang.getOrElse("en")}\">${escapeXml(text.trim)}</rdfs:label>\n</rdf:Description>"
                )
              else None

            Stream.emits(parentBlock.toList ++ valueBlock.toList)
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

  def runInferAndLexicon: IO[Unit] = {
    val in: InputStream = getClass.getResourceAsStream("/example.xml")
    if (in == null) IO.raiseError(new IllegalArgumentException("Missing example.xml"))
    else {
      val tagSet = mutable.Set[String]()
      val tagToStrings = mutable.Map[String, mutable.Set[String]]().withDefault(_ => mutable.Set())
      var stack: List[String] = Nil // Only need tag names, not IRIs

      val xmlEvents =
        fs2.io.readInputStream(IO.pure(in), 4096)
          .through(fs2.text.utf8.decode)
          .through(events[IO, String]())

      val process = xmlEvents.evalMap {
        case StartTag(qn, _, _) =>
          val tag = qn.local
          tagSet += tag
          stack = tag :: stack
          IO.unit

        case EndTag(qn) =>
          stack = stack.drop(1)
          IO.unit

        case XmlString(text, _) if text.trim.nonEmpty =>
          val str = text.trim
          stack.headOption match {
            case Some((tag: String)) => 
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

        // Output lexicon
        _ <- IO {
          val tagDir = Paths.get("tags")
          Files.createDirectories(tagDir)

          // Save the list of tag names (raw, unsanitized) to a central index
          Files.write(tagDir.resolve("tags.txt"), tagSet.toList.sorted.asJava)

          for (tag <- tagSet) {
            val sanitizedTag = sanitizeForFilename(tag)
            val lines = tagToStrings.getOrElse(tag, mutable.Set.empty).toList.sorted
            Files.write(tagDir.resolve(s"$sanitizedTag.txt"), lines.asJava)
          }
          val summaryLines = tagSet.toList.sorted.map { tag =>
            val count = tagToStrings.getOrElse(tag, mutable.Set.empty).size
            s"$tag: $count string(s)"
          }

          Files.write(Paths.get("tags/_summary.txt"), summaryLines.asJava)

        }

      } yield ()
    }
  }

}

