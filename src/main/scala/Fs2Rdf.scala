import cats.effect.{IO, IOApp}
import fs2.Stream
import fs2.data.xml._
import fs2.data.xml.XmlEvent._
import java.io.InputStream
import java.nio.file.Paths

// === Internal Representation ===
case class ElementInfo(
    qualifiedName: String,
    localName: String,
    prefix: Option[String],
    namespaceUri: Option[String]
)

object Fs2ToRdfXml extends IOApp.Simple {

  // Convert an ElementInfo into RDF/XML triples
  def toRdfTripleXml(info: ElementInfo): String =
    s"""  <rdf:Description rdf:about="${info.namespaceUri.getOrElse("")}/${info.localName}">
       |    <rdf:type rdf:resource="https://fs2.io/Element"/>
       |    <fs2:qname>${info.qualifiedName}</fs2:qname>
       |    <fs2:local>${info.localName}</fs2:local>
       |    <fs2:prefix>${info.prefix.getOrElse("")}</fs2:prefix>
       |    <fs2:namespaceUri>${info.namespaceUri.getOrElse("")}</fs2:namespaceUri>
       |  </rdf:Description>""".stripMargin

  // Extract relevant context and build RDF representation
  def liftEvent(context: Map[String, String]): XmlEvent => (Stream[IO, String], Map[String, String]) = {
    case StartTag(qn, attrs, _) =>
      val newMappings = attrs.collect {
        case Attr(QName(Some("xmlns"), prefix), value) =>
          prefix -> value.collect { case XmlString(s, _) => s }.mkString
        case Attr(QName(None, "xmlns"), value) =>
          "" -> value.collect { case XmlString(s, _) => s }.mkString
      }.toMap

      val updatedCtx = context ++ newMappings
      val nsUri = qn.prefix.flatMap(updatedCtx.get)

      val info = ElementInfo(
        qualifiedName = qn.prefix.map(p => s"$p:${qn.local}").getOrElse(qn.local),
        localName = qn.local,
        prefix = qn.prefix,
        namespaceUri = nsUri
      )

      (Stream.emit(toRdfTripleXml(info)), updatedCtx)

    case _ => (Stream.empty, context)
  }

  def run: IO[Unit] = {
    val in: InputStream = getClass.getResourceAsStream("/example.xml")
    if (in == null) IO.raiseError(new IllegalArgumentException("Missing example.xml"))
    else {
      val xmlEvents =
        fs2.io.readInputStream(IO.pure(in), 4096)
          .through(fs2.text.utf8.decode)
          .through(events[IO, String]())

      val rdfTriples = xmlEvents
        .evalScan((Stream.empty.covaryAll[IO, String], Map.empty[String, String])) {
          case ((_, ctx), event) =>
            val (stream, newCtx) = liftEvent(ctx)(event)
            IO.pure((stream, newCtx))
        }
        .flatMap(_._1)

      val rdfOutput =
        Stream.emit("""<?xml version="1.0" encoding="UTF-8"?>
          <rdf:RDF xmlns:fs2="https://fs2.io/" xmlns:ex="http://example.org/ns/" xmlns:meta="http://example.org/meta/"
         xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">""") ++
          rdfTriples ++
          Stream.emit("</rdf:RDF>")

      rdfOutput
        .intersperse("\n")
        .through(fs2.text.utf8.encode)
        .through(fs2.io.file.Files[IO].writeAll(Paths.get("example.rdf")))
        .compile
        .drain
    }
  }
}
