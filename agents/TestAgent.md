# Agent: TestAgent

## Purpose

Develop and maintain a test suite that validates that all RDF/XML output conforms to:

- The [RDF/XML syntax specification](src/main/resources/rdf-1.1-XML-Syntax.html)
- The semantic lifting rules defined in `XmlToRdfAgent.md`
- The behavior implied by the example input file (`example.xml`)

## Role

When Codex is modifying or refactoring RDF/XML logic, it should:

- Check for a corresponding test or write one if missing
- Validate output using an RDF parser if available
- Compare the parsed graph with expected rdf/xml statements
- Treat `src/test/scala/XmlToRdfSpec.scala` as the home for compliance tests

## Tools

- Testing framework: MUnit (via `org.scalameta %% munit % 1.0.0`)
- RDF/XML validation: if feasible, parse with `banana-rdf` or an XML validator
- Codex is allowed to simulate rdf/xml statement matching even without a parser, using substring checks if needed

## Example Test Ideas

- `example.xml` should produce a `rdf:type :Book` rdf/xml representation for `:bk101`
- Each nested element in `<book>` should be emitted as both `rdfs:member` and `hasX` properties
- String literals should be normalized to IRIs (e.g., `Gambardella, Matthew` → `:GambardellaMatthew`)
- RDF/XML syntax should begin with `<rdf:RDF` and contain namespace declarations
