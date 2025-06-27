
# Agent: XmlToRdf

## Summary

This agent transforms XML documents into RDF/XML using FS2 and `fs2-data-xml` in pure Scala 3. Its core responsibility is to stream `XmlEvent`s and produce RDF/XML output conforming to the official RDF/XML syntax specification.

## Core File

* `src/main/scala/XmlToRdf.scala`: Main transformation logic
* Input: `example.xml`
* Output: `example.rdf`

## Output Format Specification

All RDF/XML output **must** conform to the W3C RDF/XML specification:

```
src/main/resources/rdf-1.1-XML-Syntax.html
```

This is the canonical formatting reference. No output should violate its rules.

## Input Semantics

The input XML is treated as an ontologically structured document.

Example:

```xml
<book id="bk101">
  <author>Gambardella, Matthew</author>
</book>
```

### Interpretation:

* `book` → OWL `Class`
* `id="bk101"` → Named individual `:bk101`
* `:bk101 rdf:type :Book`
* `author` → OWL `Class` and member of `book`
* `"Gambardella, Matthew"` → Named individual of type `:Author`
* `:bk101 rdfs:member :GambardellaMatthew`
* `:bk101 :hasAuthor :GambardellaMatthew`

## Semantic Lifting Guidelines

1. **Element Classing**

   * Treat each element name as an OWL class.

2. **Named Individuals**

   * Elements with an `id` attribute are named individuals.
   * Their tag becomes the `rdf:type`.

3. **Membership**

   * Children of a node imply `rdfs:member` relationships.

4. **Property Generation**

   * Each child element yields a property `:hasX`.

     * Domain: parent type
     * Range: child type

5. **Literal Normalization**

   * Convert strings like `"Gambardella, Matthew"` to `:GambardellaMatthew`.
   * Strip punctuation, collapse whitespace, and ensure IRI safety.

6. **rdf/xml statement Emission**

   * Each rdf/xml statement must be serialized in correct RDF/XML syntax (e.g., use `rdf:Description`, `rdf:about`, `rdf:resource`).

## Agent Behavior

* Operate as a pure `fs2.Stream[IO, Byte]` pipeline.
* Match on `XmlEvent`s to emit RDF/XML constructs.
* Maintain streaming structure: no buffering or DOM-like loading.
* Ensure valid nesting, closing tags, and namespace declarations.
* Emit prefix bindings if needed (e.g., `xmlns:rdf`, `xmlns:ex`).

## Codex Agent Expectations

* When rewriting `XmlToRdf.scala`, always validate output against RDF/XML syntax.
* Refer to `Fs2XmlDoc.md` for `XmlEvent` model.
* Avoid Java libraries, mutable state, or blocking IO.
* Use idiomatic Scala 3 (extension methods, givens, etc.).
* Prioritize functional readability and composition.

## Future Goals

* Add support for Turtle and JSON-LD as alternative serialization backends.
* Refactor rdf/xml statement emission into reusable encoder logic.
* Introduce schema-based enrichment (e.g., OWL restrictions, rdfs\:subClassOf).
* Enable streaming validation of output RDF/XML.
