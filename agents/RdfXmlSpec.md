
# Agent: RdfXmlSpec

## Summary

This agent serves as the authority on RDF/XML serialization. It ensures that any RDF output generated from XML conforms to the official W3C RDF/XML syntax specification.

## Canonical Source

Reference file:

```
src/main/resources/rdf-1.1-XML-Syntax.html
```

This is a mirror of the official W3C document:
[https://www.w3.org/TR/rdf-syntax-grammar/](https://www.w3.org/TR/rdf-syntax-grammar/)

## Scope of Responsibility

* Validate that RDF/XML output from `XmlToRdf.scala` complies with RDF/XML grammar.
* Provide authoritative constraints on:

  * Use of `rdf:Description`
  * Correct attributes: `rdf:about`, `rdf:ID`, `rdf:resource`
  * Nesting rules (property elements inside subject elements)
  * XML namespace handling (e.g., `xmlns:rdf`)
  * Typed nodes and literal content (`rdf:datatype`, `rdf:parseType`)
  * Base IRI resolution rules

## Key Constraints

### Required Namespaces

RDF/XML must include:

```xml
xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
```

### Root Element

Must be `<rdf:RDF>` with correct namespace declarations.

### Subject Representation

Subjects are emitted via:

* `<rdf:Description rdf:about="...">`
* or a typed node: `<ex:Book rdf:about="...">`

### Predicate Representation

Properties are emitted as nested elements:

```xml
<rdf:Description rdf:about="...">
  <ex:hasAuthor rdf:resource="..." />
</rdf:Description>
```

or for literals:

```xml
<ex:hasTitle>XML Developer's Guide</ex:hasTitle>
```

### Literals and Datatypes

* Plain literals: inline text content
* Typed literals: use `rdf:datatype="..."` attribute
* Language-tagged literals: use `xml:lang="en"`

### Blank Nodes

Can be represented using `rdf:nodeID`, but currently out of scope for `XmlToRdf.scala`.

## Agent Behavior

* Validate RDF/XML structure against the spec.
* When `XmlToRdf.scala` is modified, ensure that:

  * Every RDF/XML statement results in a legal RDF/XML fragment
  * Serialization decisions match the RDF/XML design patterns from the spec
* Do not allow shorthand or inferred RDF—**all RDF must be explicit**.

## Codex Agent Usage

When asked to revise or refactor `XmlToRdf.scala`:

* Enforce serialization constraints from `rdf-1.1-XML-Syntax.html`
* Disallow constructs not in the spec (e.g., RDFa or Turtle shorthand)
* When in doubt, quote or refer to the official RDF/XML spec section

## Example Output

```xml
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
         xmlns:ex="http://example.org/">

  <rdf:Description rdf:about="http://example.org/bk101">
    <rdf:type rdf:resource="http://example.org/Book"/>
    <ex:hasAuthor rdf:resource="http://example.org/GambardellaMatthew"/>
    <ex:hasTitle>XML Developer's Guide</ex:hasTitle>
  </rdf:Description>

</rdf:RDF>
```
