// For more information on writing tests, see
// https://scalameta.org/munit/docs/getting-started.html
import cats.effect.unsafe.implicits.global

class XmlToRdfTest extends munit.FunSuite {
  test("IRIs expanded and prefix declared") {
    XmlToRdf.run.unsafeRunSync()
    val rdf = scala.io.Source.fromFile("example.rdf").mkString
    assert(rdf.contains("rdf:about=\"http://example.org/"))
    assert(rdf.contains("rdf:resource=\"http://example.org/"))
    assert(rdf.contains("xmlns:ex=\"http://example.org/\""))
    assert(!rdf.contains("rdf:about=\"ex:"))
    assert(!rdf.contains("rdf:resource=\"ex:"))
  }

  test("syntactic nodes emitted") {
    XmlToRdf.run.unsafeRunSync()
    val rdf = scala.io.Source.fromFile("example.rdf").mkString
    assert(rdf.contains("ex:attribute rdf:resource"))
    assert(rdf.contains("ex:xmlString rdf:datatype"))
  }

  test("rdf can be lowered back to xml") {
    XmlToRdf.run.unsafeRunSync()
    RdfToXml.run(Nil).unsafeRunSync()
    val rdf = scala.io.Source.fromFile("example.rdf").mkString
    val xml = scala.io.Source.fromFile("lowered.xml").mkString
    assert(rdf.contains("rdf:type rdf:resource=\"http://example.org/xmlTag\""))
    assert(rdf.contains("rdf:type rdf:resource=\"http://example.org/xmlAttribute\""))
    assert(rdf.contains("ex:attribute rdf:resource"))
    assert(rdf.contains("ex:xmlString rdf:datatype"))
  }

}
