# Agent: TestAgent

## Purpose

This agent is responsible for generating, maintaining, and evolving test cases that ensure the correctness of the XML-to-RDF transformation. Tests should validate both syntactic conformance (RDF/XML format per spec) and semantic accuracy (ontology-compliant lifting of XML).

## Scope of Testing

1. **Structural Validity**
   - Confirm generated RDF/XML conforms to [W3C RDF/XML Syntax Specification](../resources/rdf-1.1-XML-Syntax.html)
   - Ensure that RDF/XML serialization is well-formed and parsable by conformant RDF parsers
   - Validate namespace declarations, use of `rdf:Description`, `rdf:about`, and container elements

2. **Semantic Lifting Rules**
   - Verify that:
     - Each element is mapped to an `rdf:type` with a corresponding OWL class
     - Child elements yield both `rdfs:member` and `hasX` properties
     - String literals are normalized to safe IRIs (e.g., `Gambardella, Matthew` → `:GambardellaMatthew`)
     - URIs are reused consistently across the output

3. **Stream & Transformation Logic**
   - Tests should assert that FS2 streaming correctly emits all expected RDF/XML statements, without duplication or loss
   - Edge cases (e.g., missing `@id`, deeply nested elements, empty tags, multivalued properties) must be tested

4. **Round-Trip Compliance (Optional)**
   - If feasible, test RDF/XML output by re-parsing with an RDF/XML parser and comparing with expected RDF/XML statement sets

## Framework

Use [MUnit](https://scalameta.org/munit/) as the Scala 3-compatible test framework:

```scala
// Include in build.sbt if not already present
libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test
