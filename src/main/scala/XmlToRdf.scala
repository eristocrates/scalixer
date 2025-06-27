import cats.effect.{IO, IOApp}
import fs2.Stream
import fs2.data.xml._
import fs2.data.xml.XmlEvent._
import java.io.InputStream
import java.nio.file.Paths
import scala.util.hashing.MurmurHash3
import scala.collection.immutable.ListMap

object XmlToRdf extends IOApp.Simple {

  private val prefixMap: Map[String, String] = ListMap(
    "rdf"  -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
    "owl"  -> "http://www.w3.org/2002/07/owl#",
    "ex"   -> "http://example.org/"
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


  private def createHasProperty(tag: String): String =
    s"ex:has${pascalCase(tag)}"

  private def createSyntacticIRI(tag: String, stack: List[(String, String)]): String =
    expandPrefix(s"ex:${tag}_${MurmurHash3.stringHash((tag :: stack.map(_._2)).mkString("/"))}")

  private def createSemanticIRI(value: String): String =
    expandPrefix(s"ex:${pascalSnakeCase(value)}")

  private def syntacticClassIRI(tag: String): String =
    expandPrefix(s"ex:${pascalCase(tag)}_Tag")

  private def semanticClassIRI(tag: String): String =
    expandPrefix(s"ex:${pascalCase(tag)}")

  private def escapeXml(s: String): String =
    s.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")

  def liftEvent(
      lang: Option[String]
  ): XmlEvent => Stream[IO, String] = {
    var stack: List[(String, String)]    = Nil
    var emittedClasses: Set[String]      = Set.empty
    var emittedSemantic: Set[String]     = Set.empty

    {
      case StartTag(qn, attrs, _) =>
        val tag           = qn.local
        val synClassIRI   = syntacticClassIRI(tag)
        val semClassIRI   = semanticClassIRI(tag)

        val classBlocks =
          if (!emittedClasses.contains(tag)) then
            emittedClasses += tag
            List(
              s"<rdf:Description rdf:about=\"$synClassIRI\">\n  <rdf:type rdf:resource=\"${expandPrefix("owl:Class")}\"/>\n</rdf:Description>",
              s"<rdf:Description rdf:about=\"$semClassIRI\">\n  <rdf:type rdf:resource=\"${expandPrefix("owl:Class")}\"/>\n</rdf:Description>"
            )
          else Nil

        val subjectIRI = createSyntacticIRI(tag, stack)

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
            val classIRI  = semanticClassIRI(tag)
            val hasProp   = createHasProperty(tag)
            val parentIRI = stack.drop(1).headOption.map(_._1)

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

      val rdfStream = xmlEvents.flatMap(liftEvent(Some("en")))

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
}

