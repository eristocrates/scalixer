import munit.FunSuite

class LexiconAgentTest extends FunSuite {
  test("infer anyURI for scheme-like strings") {
    assertEquals(LexiconAgent.inferLiteralType("http://example.com"), "xsd:anyURI")
    assertEquals(LexiconAgent.inferLiteralType("mailto:me@example.com"), "xsd:anyURI")
    assertEquals(LexiconAgent.inferLiteralType("at://user/123"), "xsd:anyURI")
  }

  test("fallback to string for non-URI") {
    assertEquals(LexiconAgent.inferLiteralType("just text"), "xsd:string")
  }
}