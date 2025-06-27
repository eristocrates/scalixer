import cats.effect.{IO, IOApp}
import fs2.Stream
import fs2.data.xml._
import fs2.data.xml.XmlEvent._
import java.io.InputStream
import java.nio.file.Paths
import scala.util.hashing.MurmurHash3

object XmlToRdf extends IOApp.Simple {

  val rdfHeader =
    """<?xml version="1.0"?>
      |<rdf:RDF
      |  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
      |  xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
      |  xmlns:owl="http://www.w3.org/2002/07/owl#"
      |  xmlns:ex="http://example.org/">
      |""".stripMargin

  val rdfFooter = "\n</rdf:RDF>"

  private def normalizeLiteral(value: String): String =
    value.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "")

  private def pascalCase(value: String): String =
    value
      .split("[^\\p{IsAlphabetic}\\p{IsDigit}]+")
      .filter(_.nonEmpty)
      .map(word => word.head.toUpper + word.tail)
      .mkString

  private def createIndividualIRI(tag: String, value: String): String =
    s"ex:${normalizeLiteral(value)}"

  private def createClassIRI(tag: String): String =
    s"ex:${pascalCase(tag)}"

  private def createHasProperty(tag: String): String =
    s"ex:has${pascalCase(tag)}"

  private def escapeXml(s: String): String =
    s.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")

  def liftEvent(
      lang: Option[String]
  ): XmlEvent => Stream[IO, String] = {
    var stack: List[(String, String)] = Nil

    {
      case StartTag(qn, attrs, _) =>
        val tag      = qn.local
        val classIRI = createClassIRI(tag)

        val idOpt = attrs.collectFirst {
          case Attr(QName(_, "id"), value) =>
            value.collect { case XmlString(s, _) => s }.mkString
        }

        val subjectIRI = idOpt match {
          case Some(id) => s"ex:$id"
          case None     => s"ex:${tag}_${MurmurHash3.stringHash(stack.map(_._1).mkString("/"))}"
        }

        val parentBlock = stack.headOption.map { case (parentIRI, _) =>
          val hasProp = createHasProperty(tag)
          s"<rdf:Description rdf:about=\"$parentIRI\">\n  <rdfs:member rdf:resource=\"$subjectIRI\"/>\n  <$hasProp rdf:resource=\"$subjectIRI\"/>\n</rdf:Description>"
        }

        val attrLines = attrs.collect {
          case Attr(QName(_, "id"), _)  => None
          case Attr(QName(_, "lang"), _) => None
          case Attr(name, value) =>
            val attrVal = value.collect { case XmlString(s, _) => s }.mkString
            val prop    = createHasProperty(name.local)
            Some(s"  <$prop>${escapeXml(attrVal)}</$prop>")
        }.flatten

        val subjectBlock =
          (List(s"<rdf:Description rdf:about=\"$subjectIRI\">", s"  <rdf:type rdf:resource=\"$classIRI\"/>") ++
            attrLines ++
            List("</rdf:Description>")).mkString("\n")

        stack = (subjectIRI, tag) :: stack

        Stream.emits(parentBlock.toList :+ subjectBlock)

        case XmlString(text, _) if text.trim.nonEmpty =>
          stack.headOption match {
            case Some((subj, tag)) =>
              val valueIRI = createIndividualIRI(tag, text.trim)
              val classIRI = createClassIRI(tag)
              val hasProp  = createHasProperty(tag)

              val parentBlock =
                s"<rdf:Description rdf:about=\"$subj\">\n  <rdfs:member rdf:resource=\"$valueIRI\"/>\n  <$hasProp rdf:resource=\"$valueIRI\"/>\n</rdf:Description>"

              val valueBlock =
                s"<rdf:Description rdf:about=\"$valueIRI\">\n  <rdf:type rdf:resource=\"$classIRI\"/>\n  <rdfs:label xml:lang=\"${lang.getOrElse("en")}\">${escapeXml(text.trim)}</rdfs:label>\n</rdf:Description>"

              Stream.emit(parentBlock) ++ Stream.emit(valueBlock)
            case None => Stream.empty
          }

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

