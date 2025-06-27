// For more information on writing tests, see
// https://scalameta.org/munit/docs/getting-started.html
import cats.effect.unsafe.implicits.global

class XmlToRdfTest extends munit.FunSuite {
  test("IRIs expanded in rdf:about") {
    XmlToRdf.run.unsafeRunSync()
    val rdf = scala.io.Source.fromFile("example.rdf").mkString
    assert(rdf.contains("rdf:about=\"http://example.org/"))
    assert(!rdf.contains("rdf:about=\"ex:"))
  }
}
