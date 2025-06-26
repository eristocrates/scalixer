@main def extractXfo(): Unit =
  val factory = SAXParserFactory.newInstance()
  factory.setNamespaceAware(true)

  val parser = factory.newSAXParser()
  val reader = parser.getXMLReader

  val handler = new InfosetHandler()
  reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler)
  reader.setContentHandler(handler)
  reader.parse(new org.xml.sax.InputSource(new java.io.FileReader("example.xml")))
