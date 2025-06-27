import cats.effect.{IO, IOApp}
import fs2.Stream
import fs2.data.xml._
import fs2.data.xml.XmlEvent._
import fs2.data.xml.render._
import java.io.InputStream
import java.nio.file.Paths
import scala.util.hashing.MurmurHash3

object XmlToRdf extends IOApp.Simple {


  private def normalizeLiteral(value: String): String =
    value.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "")

  private def pascalCase(value: String): String =
    value
      .split("[^\\p{IsAlphabetic}\\p{IsDigit}]+")
      .filter(_.nonEmpty)
      .map(word => word.head.toUpper + word.tail)
      .mkString

  private val exPrefix = "http://example.org/"

  private def createIndividualIRI(tag: String, value: String): String =
    s"${exPrefix}${normalizeLiteral(value)}"

  private def createClassIRI(tag: String): String =
    s"${exPrefix}${pascalCase(tag)}"

  private def createHasProperty(tag: String): String =
    s"ex:has${pascalCase(tag)}"

  private def attr(prefix: String, local: String, value: String): Attr =
    Attr(QName(Some(prefix), local), List(XmlString(value, false)))

  private def emptyElem(name: QName, attrs: List[Attr]): List[XmlEvent] =
    List(StartTag(name, attrs, true))

  private def elem(name: QName, attrs: List[Attr], children: List[XmlEvent]): List[XmlEvent] =
    StartTag(name, attrs, false) :: children ::: List(EndTag(name))

  private def description(subject: String, children: List[XmlEvent]): List[XmlEvent] =
    StartTag(QName(Some("rdf"), "Description"), List(attr("rdf", "about", subject)), false) ::
      children :::
      List(EndTag(QName(Some("rdf"), "Description")))


  def liftEvent(
      lang: Option[String]
  ): XmlEvent => Stream[IO, XmlEvent] = {
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
          case Some(id) => s"${exPrefix}$id"
          case None     => s"${exPrefix}${tag}_${MurmurHash3.stringHash(stack.map(_._1).mkString("/"))}"
        }

        val parentBlock = stack.headOption.toList.flatMap { case (parentIRI, _) =>
          val hasProp = createHasProperty(tag)
          description(
            parentIRI,
            emptyElem(QName(Some("rdfs"), "member"), List(attr("rdf", "resource", subjectIRI))) ++
              emptyElem(QName(hasProp), List(attr("rdf", "resource", subjectIRI)))
          )
        }

        val attrLines = attrs.collect {
          case Attr(QName(_, "id"), _)  => None
          case Attr(QName(_, "lang"), _) => None
          case Attr(name, value) =>
            val attrVal = value.collect { case XmlString(s, _) => s }.mkString
            val prop    = createHasProperty(name.local)
            Some(
              elem(
                QName(prop),
                Nil,
                List(XmlString(attrVal, false))
              )
            )
        }.flatten.flatten

        val subjectBlock = description(
          subjectIRI,
          emptyElem(QName(Some("rdf"), "type"), List(attr("rdf", "resource", classIRI))) ++
            attrLines
        )

        stack = (subjectIRI, tag) :: stack

        Stream.emits(parentBlock ++ subjectBlock)

      case XmlString(text, _) if text.trim.nonEmpty =>
        stack.headOption match {
          case Some((subj, tag)) =>
            val valueIRI = createIndividualIRI(tag, text.trim)
            val classIRI = createClassIRI(tag)
            val hasProp  = createHasProperty(tag)

            val parentBlock = description(
              subj,
              emptyElem(QName(Some("rdfs"), "member"), List(attr("rdf", "resource", valueIRI))) ++
                emptyElem(QName(hasProp), List(attr("rdf", "resource", valueIRI)))
            )

            val valueBlock = description(
              valueIRI,
              emptyElem(QName(Some("rdf"), "type"), List(attr("rdf", "resource", classIRI))) ++
                elem(
                  QName(Some("rdfs"), "label"),
                  List(attr("xml", "lang", lang.getOrElse("en"))),
                  List(XmlString(text.trim, false))
                )
            )

            Stream.emits(parentBlock ++ valueBlock)
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

      val rootAttrs = List(
        attr("xmlns", "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
        attr("xmlns", "rdfs", "http://www.w3.org/2000/01/rdf-schema#"),
        attr("xmlns", "owl", "http://www.w3.org/2002/07/owl#"),
        attr("xmlns", "ex", "http://example.org/")
      )

      val outputEvents =
        Stream.emit(StartDocument) ++
          Stream.emit(StartTag(QName(Some("rdf"), "RDF"), rootAttrs, false)) ++
          rdfStream ++
          Stream.emit(EndTag(QName(Some("rdf"), "RDF"))) ++
          Stream.emit(EndDocument)

      outputEvents
        .through(prettyPrint())
        .through(fs2.text.utf8.encode)
        .through(fs2.io.file.Files[IO].writeAll(Paths.get("example.rdf")))
        .compile
        .drain
    }
  }
}

