
# Agent: OntologyLifting

## Summary

This agent is responsible for semantically enriching parsed XML content into **valid RDF/XML**, using role-based interpretation to structure and annotate the output as meaningful OWL constructs. It relies on **FS2 streaming** and idiomatic **Scala 3** patterns throughout.

Its mission is to **lift** XML structures into a semantically rich RDF/XML representation — where *lifting* refers to the process of mapping structural XML features into higher-level OWL semantics using external role configuration files.

While syntactic RDF/XML is always emitted (structural by default), **semantic RDF/XML** is generated only when tags and strings are explicitly staged via role files.

## Responsibilities

* Interpret XML as OWL individuals, properties, or classes depending on context
* Emit **standards-compliant RDF/XML**
* Support two complementary layers:

  * **Syntactic RDF/XML** — structural output always emitted
  * **Semantic RDF/XML** — selectively emitted when roles are configured
* Ensure every XML tag is faithfully represented as an RDF/XML node
* Promote attributes and string values based on type inference and string roles

## Role-Based Lifting Strategy

### TagRole Mapping

| TagRole         | Purpose                                            | RDF/XML Behavior                                                                 |
| --------------- | -------------------------------------------------- | -------------------------------------------------------------------------------- |
| `EntityTag`     | Denotes a discrete object or instance              | Emits an RDF/XML individual with a generated IRI and class-suffixed type         |
| `PropertyTag`   | Tag represents a predicate/property                | Emits an RDF/XML property between subject and child, based on structural context |
| `ContainerTag`  | Groups multiple children; often a helper structure | May be skipped as an RDF/XML node; children lifted to grandparent                |
| `TypenameTag`   | Tag encodes a class (often via `xsi:type`)         | Results in a `rdf:type` element inside RDF/XML                                   |
| `ReferenceTag`  | Tag is a reference to another IRI                  | Emits a semantic reference (e.g., via `rdf:resource`)                            |
| `AnnotationTag` | Tag provides descriptive metadata                  | Mapped to `rdfs:comment`, `skos:note`, or similar annotations                    |
| `CollectionTag` | Encodes a sequence, bag, or set of values          | Emits RDF/XML using `rdf:Bag`, `rdf:Seq`, etc. with appropriate `rdf:_n` members |

### StringRole Mapping

| StringRole           | Purpose                                              | RDF/XML Behavior                            |
| -------------------- | ---------------------------------------------------- | ------------------------------------------- |
| `LabelString`        | User-friendly label for an individual or class       | Emits `<rdfs:label>` with literal content   |
| `LiteralValueString` | General fallback for scalar values                   | Emits value as an RDF/XML literal           |
| `IdentifierString`   | Used to construct an IRI                             | IRI derived from value, used in `rdf:about` |
| `ReferenceString`    | String intended as an IRI pointer                    | Used with `rdf:resource` in RDF/XML         |
| `ClassValueString`   | String denotes a class or vocabulary-defined concept | Used in `<rdf:type rdf:resource="..."/>`    |
| `EmptyString`        | Placeholder or missing                               | Ignored or flagged for diagnostic output    |
| `MixedContentString` | Contains both text and child nodes                   | Serialized using `rdf:parseType="Literal"`  |

## Syntactic RDF/XML (Default Layer)

If **no semantic roles** are specified, the agent still emits a **default RDF/XML structure** based on XML anatomy:

* Each element is serialized as an RDF individual, typed with `:<TagName>_Tag`
* Parent-child relationships are expressed using `rdfs:member`
* Attribute values are serialized as literal property elements
* Output strictly adheres to the [RDF/XML W3C specification]

This ensures the RDF/XML document is **well-formed, valid, and navigable**, even without semantic enrichment.

[RDF/XML W3C specification]: ./src/main/resources/rdf-1.1-XML-Syntax.html

## Semantic RDF/XML (Role-Staged Layer)

When roles are configured, the agent **lifts** specific tags and strings into higher semantic forms:

* Tag names in `EntityTag.txt` cause OWL individuals to be typed with meaningful class IRIs
* Attributes or strings in `LabelString.txt` cause `<rdfs:label>` to be emitted
* `ReferenceTag.txt` causes IRIs to be dereferenced via `rdf:resource` usage
* Combined role signals (e.g., `PropertyTag` and `LiteralValueString`) allow enriched RDF/XML with predicates and typed literals

## Attribute Handling

* Every XML attribute is serialized into RDF/XML as a predicate
* If the attribute name has a configured TagRole, it influences the property IRI and typing
* If the attribute value has an inferred XSD datatype, it is emitted using a typed literal
* String roles refine how the value is rendered (e.g., label vs. identifier vs. reference)

## Compliance & Strategy

* All output adheres strictly to RDF/XML grammar rules
* The agent uses only **valid OWL+RDF constructs** as described in the RDF/XML spec
* FS2 streaming ensures attributes and elements are processed efficiently and incrementally
* Every tag is lifted *once*, but may result in multiple RDF/XML fragments depending on roles

## Design Guarantees

* **Composability**: Output RDF/XML can be merged or reasoned over incrementally
* **Traceability**: All RDF/XML statements can be traced back to their original XML source
* **Fallback Safety**: Even incomplete role configuration produces valid RDF/XML

## Future Considerations

* Integrate role reasoning (e.g., OWL inference for class hierarchies)
* Add support for semantic vocabularies (SKOS, DC, PROV-O) via role-to-namespace mapping
* Allow users to specify default role behavior per namespace or tag pattern

## AGENTS.md Entry

| Agent             | Responsibility |
| ----------------- | -------------- |
| `OntologyLifting` | Lifts XML events into OWL-aware RDF/XML using Scala 3 FS2 streams; emits semantic statements only when roles are staged and preserves inferred XSD types as syntactic sugar. |
