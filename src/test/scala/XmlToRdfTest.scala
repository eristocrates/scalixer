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
    assert(rdf.contains("ex:xmlString"))
  }

}
