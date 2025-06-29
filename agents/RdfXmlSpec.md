# Agent: RdfXmlSpec

## Summary

This agent ensures that all emitted RDF is serialized as **valid RDF/XML** in strict compliance with the [W3C RDF/XML Syntax Specification]. It is responsible for encoding triples into correct XML elements, managing RDF/XML-specific constructs (e.g., `rdf:Description`, `rdf:resource`, `rdf:parseType`), and avoiding illegal structures.

This agent **does not determine which triples are emitted** — that is handled by `XmlToRdf` — but it guarantees that whatever is emitted is serialized into **legal RDF/XML**, preserving semantic meaning, structural correctness, and round-trip potential.

[W3C RDF/XML Syntax Specification]: ./src/main/resources/rdf-1.1-XML-Syntax.html

## Responsibilities

* Encode all RDF content using valid RDF/XML constructs
* Serialize both syntactic and semantic RDF structures as XML
* Handle language tags, datatypes, and IRI escaping
* Avoid reserved keyword misuse (e.g., illegal attributes, tag nesting)
* Ensure compact or verbose RDF/XML is chosen consistently per context

## Core Concepts

### Emission Scope

This agent assumes RDF content is already **staged** — either as:

* **Syntactic RDF/XML**, directly reflecting the XML tag and attribute structure
* **Semantic RDF/XML**, based on user-configured tag and string roles

The agent must handle both cases **uniformly** by emitting only standards-compliant RDF/XML elements.

### Canonical Mapping Patterns

| RDF Concept             | RDF/XML Element Form                                            |
| ----------------------- | --------------------------------------------------------------- |
| Triple                  | `<rdf:Description rdf:about="...">...</rdf:Description>`        |
| Object Property         | `<:property><rdf:Description rdf:about="..."/></:property>`     |
| Data Property           | `<:property>LiteralValue</:property>`                           |
| Class Assertion         | `<rdf:type rdf:resource="..."/>` or `rdf:type="..."` on subject |
| rdfs\:label             | `<rdfs:label xml:lang="en">Label</rdfs:label>`                  |
| Reference Link          | `<:property rdf:resource="IRI"/>`                               |
| Collection              | `<rdf:Bag><rdf:li rdf:resource="..."/></rdf:Bag>`               |
| Language-tagged literal | `<:property xml:lang="en">Text</:property>`                     |
| Typed literal           | `<:property rdf:datatype="xsd:boolean">true</:property>`        |

### Compliance Tasks

* **IRIs**: Ensure valid QName or use full IRI as `rdf:resource`
* **Datatypes**: Attach `rdf:datatype="..."` only when valid
* **Language**: Inject `xml:lang` when `lang` attribute or role is detected
* **Ordering**: Maintain element order when semantic roles require sequence (`rdf:Seq`)
* **Tag Safety**: Prevent nesting of `rdf:Description` inside non-container elements

## String Handling

This agent delegates string interpretation to `XmlToRdf`, but it must handle:

* Escaping of special characters (e.g., `&`, `<`, `>`, `"`)
* Preservation of whitespace if `StringRole` indicates `MixedContentString`
* Unicode characters as valid XML

## Namespace Management

All RDF/XML output must declare appropriate namespaces:

* `xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"`
* Others as required by tag prefixes (e.g., `xmlns:ex="http://example.org/"`)

Prefixes must be **valid NCNames**, and **unused prefixes should be omitted**.

## Error Sensitivity

This agent will:

* **Reject** invalid RDF/XML structures
* **Fail fast** on unclosed tags, missing namespace declarations, or disallowed nesting
* **Log** incomplete or malformed triples, but never emit illegal RDF/XML

It must not:

* Attempt to "fix" invalid RDF content — upstream logic is responsible for correctness
* Introduce implicit semantics or guesses

## Streaming Considerations

The RDF/XML output must be:

* Fully streamable, via FS2 Sink or Writer
* Emitted as a flat stream of `String` segments or serialized XML events
* Capable of writing to file or console without buffering

## Design Philosophy

* **Strict adherence** to RDF/XML spec
* **No semantic guessing**: serialize what is given, correctly
* **Streaming-oriented**: support RDF/XML emission from FS2 pipelines
* **Schema-flexible**: work with any namespace or vocabulary
* **Separation of Concerns**: does not infer triples, only serializes them