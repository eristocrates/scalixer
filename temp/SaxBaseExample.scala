import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.{Attributes, InputSource}
import javax.xml.parsers.SAXParserFactory
import java.io.File

object SaxExample:

  def main(args: Array[String]): Unit =
    val file = File("src/main/resources/sample.xml") // Create this XML file!
    val factory = SAXParserFactory.newInstance()
    val parser = factory.newSAXParser()
    val reader = parser.getXMLReader

    val handler = new DefaultHandler:
      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit =
        println(s"Start Element: <$qName>")
      override def endElement(uri: String, localName: String, qName: String): Unit =
        println(s"End Element: </$qName>")
      override def characters(ch: Array[Char], start: Int, length: Int): Unit =
        val text = new String(ch, start, length).trim
        if text.nonEmpty then println(s"Text: $text")

    reader.setContentHandler(handler)
    reader.parse(InputSource(file.toURI.toString))
