import java.io.FileInputStream
import javax.xml.parsers.SAXParserFactory
import org.xml.sax._
import org.xml.sax.ext.LexicalHandler
import org.xml.sax.helpers.DefaultHandler

@main def saxInfosetLogger(xmlPath: String): Unit =
  val factory = SAXParserFactory.newInstance()
  factory.setNamespaceAware(true)

  val parser = factory.newSAXParser()
  val reader = parser.getXMLReader

  val handler = new InfosetHandler
  reader.setContentHandler(handler)
  reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler)

  val input = new FileInputStream(xmlPath)
  reader.parse(new InputSource(input))

class InfosetHandler extends DefaultHandler with LexicalHandler:
  override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit =
    println(s"Start Element: <$qName>")

  override def endElement(uri: String, localName: String, qName: String): Unit =
    println(s"End Element: </$qName>")

  override def characters(ch: Array[Char], start: Int, length: Int): Unit =
    val text = new String(ch, start, length).trim
    if text.nonEmpty then println(s"Text: $text")

  override def comment(ch: Array[Char], start: Int, length: Int): Unit =
    val comment = new String(ch, start, length).trim
    println(s"Comment: <!-- $comment -->")

  override def startDTD(name: String, publicId: String, systemId: String): Unit = ()
  override def endDTD(): Unit = ()
  override def startEntity(name: String): Unit = ()
  override def endEntity(name: String): Unit = ()
  override def startCDATA(): Unit = ()
  override def endCDATA(): Unit = ()
