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

  def normalize(value: String): String =
    value.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "").capitalize

  def createIndividualIRI(tag: String, value: String): String =
    s"ex:${normalize(value)}"

  def createClassIRI(tag: String): String =
    s"ex:${normalize(tag)}"

  def createHasProperty(tag: String): String =
    s"ex:has${tag.capitalize}"

  def emitTriple(s: String, p: String, o: String, literal: Boolean = false, lang: Option[String] = None): String = {
    val obj = if (literal) {
      val langTag = lang.map(l => s" xml:lang=\"$l\"").getOrElse("")
      s"\"$o\"$langTag"
    } else o
    s"<$s> <$p> $obj ."
  }

  def liftEvent(
      lang: Option[String]
  ): XmlEvent => Stream[IO, String] = {
    var currentSubject: Option[String] = None
    var parentStack: List[String] = Nil

    {
      case StartTag(qn, attrs, _) =>
        val tag = qn.local
        val classIRI = createClassIRI(tag)

        val idOpt = attrs.collectFirst {
          case Attr(QName(_, "id"), value) =>
            value.collect { case XmlString(s, _) => s }.mkString
        }

        val subjectIRI = idOpt match {
          case Some(id) => s"ex:$id"
          case None     => s"ex:${tag}_${MurmurHash3.stringHash(parentStack.mkString("/"))}"
        }

        val classDecl = s"<$classIRI> a owl:Class ."
        val instanceDecl = s"<$subjectIRI> a <$classIRI> ."

        val membership = parentStack.headOption.map { parent =>
          s"<$parent> <rdfs:member> <$subjectIRI> ."
        }

        val attrTriples = attrs.map {
          case Attr(QName(_, "id"), _) => None // Already used
          case Attr(QName(_, "lang"), _) => None // Will be handled by lang propagation
          case Attr(name, value) =>
            val attrVal = value.collect { case XmlString(s, _) => s }.mkString
            val propIRI = s"ex:has${name.local.capitalize}"
            Some(s"<$subjectIRI> <$propIRI> \"$attrVal\" .")
        }.flatten

        currentSubject = Some(subjectIRI)
        parentStack = subjectIRI :: parentStack

        Stream.emits((membership.toList :+ classDecl :+ instanceDecl) ++ attrTriples)

      case XmlString(text, _) if text.trim.nonEmpty =>
        currentSubject match {
          case Some(subj) =>
            val normalized = normalize(text)
            val classTag   = parentStack.headOption.map(_.split("/").lastOption.getOrElse("Unknown")).getOrElse("Unknown")
            val valueIRI   = createIndividualIRI(classTag, text.trim)
            val classIRI   = createClassIRI(classTag)
            val hasProp    = createHasProperty(classTag)

            Stream.emits(List(
              s"<$valueIRI> a <$classIRI> .",
              emitTriple(subj, "rdfs:member", valueIRI),
              emitTriple(subj, hasProp, valueIRI),
              emitTriple(valueIRI, "rdfs:label", text.trim, literal = true, lang)
            ))
          case None => Stream.empty
        }

      case EndTag(_) =>
        parentStack = parentStack.drop(1)
        currentSubject = parentStack.headOption
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

      val triplesStream = xmlEvents.flatMap(liftEvent(Some("en")))

      val output = Stream.emit(rdfHeader) ++
        triplesStream.intersperse("\n") ++
        Stream.emit(rdfFooter)

      output
        .through(fs2.text.utf8.encode)
        .through(fs2.io.file.Files[IO].writeAll(Paths.get("example.rdf")))
        .compile
        .drain
    }
  }
}

