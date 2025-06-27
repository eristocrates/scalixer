
# Agent: OntologyLifting

## Summary

This agent is responsible for semantic enrichment of XML data into RDF/XML following ontological modeling principles. It supports transformation patterns that elevate flat or nested XML into meaningful OWL constructs, including named individuals, classes, properties, and relationships.

## Context

This project parses XML using `fs2-data-xml` and streams RDF/XML using idiomatic Scala 3 and FS2. The transformation logic is implemented in `XmlToRdf.scala`. The agent works in tandem with `RdfXmlSpec` and `Fs2XmlDoc`.

## Lifting Philosophy

This agent interprets raw XML data as a **semantic document**. Its job is to infer **meaningful RDF triples**, not just direct structural mappings.

It does so by applying transformation rules that convert XML:

* Elements → Classes or Individuals
* Attributes → Identifiers or Data Properties
* Nesting → Membership or Property Relationships
* Strings → IRIs (via normalization)

## Semantic Lifting Rules

### 1. Class Inference

```xml
<book> ... </book>
```

→

```turtle
:book a owl:Class .
```

Any element name implies an OWL `Class`.

---

### 2. Named Individuals

```xml
<book id="bk101"> ... </book>
```

→

```turtle
:bk101 a :book .
```

* If `id` is present, treat the element as a named individual of the corresponding class.

---

### 3. Nested Elements → Membership

```xml
<book>
  <author>Matthew</author>
</book>
```

→

```turtle
_:book123 rdfs:member _:author456 .
```

Child elements imply `rdfs:member` relationships with the parent, which may later be strengthened by domain-specific properties.

---

### 4. Property Derivation (hasX Convention)

```xml
<book>
  <author>Matthew</author>
</book>
```

→

```turtle
:hasAuthor rdfs:domain :Book ;
           rdfs:range :Author .
:book123 :hasAuthor :Matthew .
```

From element `<author>`, derive property `:hasAuthor` based on the parent’s class.

---

### 5. Literal Normalization → IRI Identity

```xml
<author>Gambardella, Matthew</author>
```

→

```turtle
:GambardellaMatthew a :Author .
```

Normalize inner text into a safe IRI. All punctuation and whitespace are removed unless semantically relevant. For example:

| Literal Text             | IRI                   |
| ------------------------ | --------------------- |
| `"Gambardella, Matthew"` | `:GambardellaMatthew` |
| `"XML Guide"`            | `:XMLGuide`           |

---

## Agent Responsibilities

* Infer OWL structure from XML layout
* Promote IDs and values to named individuals
* Preserve document structure through semantic links (`rdfs:member`)
* Derive properties when no explicit attributes exist
* Prioritize ontological fidelity over lossy flattening

## Implementation Notes

* Codex must apply these rules **when editing XmlToRdf.scala**
* Codex may refactor helper functions (e.g., `normalizeLiteralToIri`, `derivePropertyFromChild`)
* The agent may suggest use of temporary blank nodes if no ID is present, but must avoid generating anonymous nodes unnecessarily

## Related Files

* `example.xml`: Source data
* `XmlToRdf.scala`: Transformation entrypoint
* `RdfXmlSpec.md`: Output serialization format
* `Fs2XmlDoc.md`: Underlying streaming/parsing library

## Coordination Notes

This agent delegates:

* XML streaming concerns → `Fs2XmlDoc`
* RDF/XML serialization constraints → `RdfXmlSpec`
* Scala 3 idioms and correctness → `Scala3CompilerDoc`

