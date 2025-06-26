// File: src/main/scala/XfoExtractor.scala
package scalixer

import org.xml.sax._
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.ext.LexicalHandler
import org.xml.sax.XMLReader
import javax.xml.parsers.SAXParserFactory

object XfoExtractor:
  def main(args: Array[String]): Unit =
    val filename = args.headOption.getOrElse {
      println("Usage: run <file.xml>")
      sys.exit(1)
    }

    val factory = SAXParserFactory.newInstance()
    factory.setNamespaceAware(true)
    val parser = factory.newSAXParser()
    val reader = parser.getXMLReader // ✅ THIS is how you get XMLReader

    val handler = new InfosetHandler()
    reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler)
    reader.setContentHandler(handler)
    reader.parse(new org.xml.sax.InputSource(new java.io.FileReader(filename)))

class InfosetHandler extends DefaultHandler:
  private var depth = 0

  override def startDocument(): Unit =
    println("START_DOCUMENT")

  override def endDocument(): Unit =
    println("END_DOCUMENT")

  override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit =
    println(s"${"  " * depth}START_ELEMENT: $qName")
    for i <- 0 until attributes.getLength() do
      val attrName = attributes.getQName(i)
      val attrValue = attributes.getValue(i)
      println(s"${"  " * (depth + 1)}@ATTR: $attrName = \"$attrValue\"")
    depth += 1

  override def endElement(uri: String, localName: String, qName: String): Unit =
    depth -= 1
    println(s"${"  " * depth}END_ELEMENT: $qName")

  override def characters(ch: Array[Char], start: Int, length: Int): Unit =
    val content = new String(ch, start, length).trim
    if content.nonEmpty then
      println(s"${"  " * depth}TEXT: \"$content\"")

  override def comment(ch: Array[Char], start: Int, length: Int): Unit =
    val content = new String(ch, start, length).trim
    println(s"${"  " * depth}COMMENT: \"$content\"")

  // Required stubs for full LexicalHandler interface
  override def startDTD(name: String, publicId: String, systemId: String): Unit = ()
