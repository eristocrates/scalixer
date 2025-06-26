import cats.effect.{IO, IOApp}
import fs2.Stream
import fs2.data.xml._
import fs2.data.xml.XmlEvent._
import java.io.InputStream

// === Infoset Representation ===
sealed trait InfosetItem

case class ElementInfo(
    qualifiedName: String,
    localName: String,
    prefix: Option[String],
    namespaceUri: Option[String],
    attributes: List[AttributeInfo]
) extends InfosetItem

case class AttributeInfo(
    qualifiedName: String,
    localName: String,
    prefix: Option[String],
    namespaceUri: Option[String],
    value: String
) extends InfosetItem

case class CharacterInfo(text: String) extends InfosetItem
case class CommentInfo(comment: String) extends InfosetItem
case object EndElementInfo extends InfosetItem

// === QName Extension Method ===
extension (qn: QName)
  def formatted: String = qn.prefix.map(p => s"$p:${qn.local}").getOrElse(qn.local)

object Fs2Xfo extends IOApp.Simple {

  def liftEvent: XmlEvent => Stream[IO, InfosetItem] = {
    case StartTag(qn, attrs, _) =>
      val attrItems = attrs.map { a =>
        AttributeInfo(
          qualifiedName = a.name.formatted,
          localName = a.name.local,
          prefix = a.name.prefix,
          namespaceUri = None,
          value = a.value.collect { case XmlEvent.XmlString(s, _) => s }.mkString
        )
      }.toList

      Stream.emit(ElementInfo(
        qualifiedName = qn.formatted,
        localName = qn.local,
        prefix = qn.prefix,
        namespaceUri = None,
        attributes = attrItems
      ))

    case EndTag(_) => Stream.emit(EndElementInfo)

    case XmlEvent.XmlString(text, _) if text.trim.nonEmpty =>
      Stream.emit(CharacterInfo(text.trim))

    case Comment(content) =>
      Stream.emit(CommentInfo(content))

    case _ => Stream.empty
  }

  def infosetToXml(item: InfosetItem): String = item match {
    case ElementInfo(qn, local, prefix, ns, attrs) =>
      val attrsXml = attrs.map { a =>
        s"""<attribute-information-item>
           |  <key>
           |    <qualified-name>${a.qualifiedName}</qualified-name>
           |    <local-name>${a.localName}</local-name>
           |    <prefix>${a.prefix.getOrElse("")}</prefix>
           |    <namespace-uri>${a.namespaceUri.getOrElse("")}</namespace-uri>
           |  </key>
           |  <value>${a.value}</value>
           |</attribute-information-item>""".stripMargin
      }.mkString("\n")

      s"""<element-information-item>
         |  <qualified-name>$qn</qualified-name>
         |  <local-name>$local</local-name>
         |  <prefix>${prefix.getOrElse("")}</prefix>
         |  <namespace-uri>${ns.getOrElse("")}</namespace-uri>
         |  <attributes>
         |$attrsXml
         |  </attributes>""".stripMargin

    case EndElementInfo => "</element-information-item>"

    case CharacterInfo(text) =>
      s"<character-information-item>$text</character-information-item>"

    case CommentInfo(comment) =>
      s"<comment-information-item>$comment</comment-information-item>"
  }

  def run: IO[Unit] = {
    val in: InputStream = getClass.getResourceAsStream("/sample.xml")
    if (in == null) IO.raiseError(new IllegalArgumentException("Missing sample.xml"))
    else {
      val xmlEvents =
        fs2.io.readInputStream(IO.pure(in), 4096)
          .through(fs2.text.utf8.decode)
          .through(events[IO, String]())

      val infoset = xmlEvents.flatMap(liftEvent)

      val xmlOutput = Stream.emit("<document-information-item>") ++
        infoset.map(infosetToXml) ++
        Stream.emit("</document-information-item>")

      xmlOutput
        .intersperse("\n")
        .through(fs2.text.utf8.encode)
        .through(fs2.io.file.Files[IO].writeAll(java.nio.file.Paths.get("xfo_output.xml")))
        .compile
        .drain
    }
  }
}
