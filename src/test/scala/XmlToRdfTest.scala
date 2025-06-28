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

  test("dual individuals emitted") {
    XmlToRdf.run.unsafeRunSync()
    val rdf = scala.io.Source.fromFile("example.rdf").mkString
    assert(rdf.contains("Author_Tag\">"))
    assert(rdf.contains("Author\">"))
    assert(rdf.contains("rdfs:member rdf:resource=\"http://example.org/author_"))
    assert(rdf.contains("ex:hasAuthor rdf:resource=\"http://example.org/Gambardella_Matthew"))
  }
}
