# Agent: XmlToRdf

## Summary

This agent orchestrates the entire **RDF/XML generation process** from XML input, using **streaming FS2 pipelines in Scala 3**. It performs the core transformation logic — parsing, staging, and serializing RDF/XML — while respecting all role-based lifting directives.

Its mission is to always emit **syntactic RDF/XML** that faithfully preserves the XML structure, while optionally emitting **semantic RDF/XML** when staging files exist under the `roles/` directory. It does **not emit raw triples**, but produces **well-formed, standards-compliant RDF/XML documents**. Inferred XSD primitive types are emitted as syntactic sugar regardless of roles.

## Responsibilities

* Parse XML documents using `fs2-data-xml`
* Emit RDF/XML nodes for every XML tag and attribute
* Incorporate semantic lifting logic when roles are available
* Separate **syntactic RDF/XML** (default, structure-preserving) from **semantic RDF/XML** (meaning-enriched)
* Enforce output conformity with the \[W3C RDF/XML specification]

## Lifting Strategy

The lifting process follows a **two-pass interpretation model**:

1. **Syntactic Emission (Always On)**:

   * Every tag becomes an RDF/XML individual with class `:<TagName>_Tag`
   * Every child is linked to its parent with `rdfs:member`
   * Attributes become properties with literal values
   * Text nodes are captured and stored for potential use

2. **Semantic Enrichment (Role-Driven)**:

   * If a tag appears in a `*.txt` role file, its behavior is modified
   * Semantic RDF/XML replaces or augments syntactic patterns
   * TagRoles (e.g., `EntityTag`, `PropertyTag`) dictate RDF/XML structure
   * StringRoles (e.g., `LabelString`, `IdentifierString`) guide how text is serialized

This enables users to **progressively annotate and stage** their data without losing structural fidelity.

## Role File Architecture

Each role type is mapped to a flat `.txt` file, one entry per line. This enables human-readable, version-controlled semantic configuration.

### Tag Roles (TagRole)

* `EntityTag.txt`: Forces tag to emit as individual with class type
* `PropertyTag.txt`: Converts tag into predicate linking parent and child
* `CollectionTag.txt`: Emits RDF container (`rdf:Bag`, `rdf:Seq`, etc.)
* Others: Typename, Reference, Annotation — per OWL semantics

### String Roles (StringRole)

* `LabelString.txt`: Adds `rdfs:label`
* `LiteralValueString.txt`: Adds literal value directly
* `ReferenceString.txt`: Used in `rdf:resource` to create links
* `ClassValueString.txt`: Adds class reference in `rdf:type`
* Other roles provide fallback or mixed content handling

## Attribute Strategy

Attributes are processed **in-line** with tag lifting:

* By default, every attribute becomes a property
* If the attribute’s key appears in a `TagRole`, its type is refined
* If the attribute’s value is in a `StringRole`, its serialization is modified
* If the value has an inferred XSD primitive type, it is typed accordingly (e.g., `xsd:boolean`, `xsd:decimal`)
* Multiple attributes of the same element can produce multiple RDF/XML property tags

## FS2 Streaming Model

All logic is implemented in a **single-pass FS2 pipeline**, with no intermediate collections. Streaming guarantees:

* **Memory safety** for large XML files
* **Composability**: logic can be split across stages
* **Parallelizability** if needed in future enhancements

The stream stages include:

```
XML Parser
   ↓
Role Loader
   ↓
Lexical Analysis (e.g., datatype inference)
   ↓
Syntactic RDF/XML Emission
   ↓
Semantic RDF/XML Enrichment
   ↓
RDF/XML Serialization
```

## Fallback Behavior

* Any tag or string **not configured** is handled as part of the syntactic RDF/XML layer
* This guarantees **zero-loss round-tripping** from XML → RDF/XML → back (potentially)
* Role configuration files act as **semantic activation switches**

## Error Handling and Edge Cases

* Events like `XmlDecl`, `Comment`, and `Whitespace` are skipped or ignored
* Unsupported node types (e.g., `PI`) are flagged and can be extended later
* Unknown attributes or invalid IRIs are logged but do not block generation

## Design Philosophy

* **Syntactic First**: Default emission guarantees preservation
* **Semantic Configurability**: User can progressively enrich via role files
* **Streaming All the Way**: No mutable state or tree-walking
* **W3C Compliance**: RDF/XML spec strictly enforced
* **Non-Ontological Baseline**: Even without OWL semantics, RDF/XML is valid and usable

## Future Work

* Introduce dynamic role inference based on schema or examples
* Output fan-out (Turtle, JSON-LD) through post-conversion
* Enrich string roles with custom vocabulary matchers (e.g., SKOS detection)
* Enable OWL reasoning stubs (e.g., subclass detection, inverse properties)

## AGENTS.md Entry

| Agent      | Responsibility |
| ---------- | -------------- |
| `XmlToRdf` | FS2-streaming orchestrator converting XML to RDF/XML. Emits syntactic RDF/XML for every document and conditional semantic RDF/XML when staging files exist under `roles/`. |
