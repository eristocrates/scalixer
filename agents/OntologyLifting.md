
### ✅ `OntologyLifting.md`

```markdown
# Agent: OntologyLifting

## Summary

This agent is responsible for the semantic enrichment of RDF generated from XML, especially by distinguishing **syntactic structures** from **semantic meaning** and aligning both with OWL constructs.

It works in tandem with `XmlToRdf` to ensure all lifted data is ontologically valid, expressive, and typed according to OWL principles.

## Responsibilities

- Model every XML element as:
  - A **syntactic class** (e.g., `Author_Tag`)
  - A **semantic class** (e.g., `Author`)
- Generate corresponding named individuals for both forms
- Ensure `rdfs:member` is used for syntactic containment
- Ensure domain-specific properties like `:hasAuthor` link semantic content

## Example Transformation

From:

```xml
<author>Gambardella, Matthew</author>
````

Generate:

* Individual: `author_1234` of type `Author_Tag`
* Individual: `Gambardella_Matthew` of type `Author`
* `rdfs:member`: subject has `author_1234`
* `ex:hasAuthor`: subject has `Gambardella_Matthew`

Also assert:

```turtle
:Author_Tag a owl:Class .
:Author a owl:Class .
:Gambardella_Matthew a :Author ;
  rdfs:label "Gambardella, Matthew"@en .
```

## Class System

* All element tags receive two OWL classes:

  * `TagName_Tag`: syntactic class
  * `TagName`: semantic class

* All IRIs are generated with namespace prefix expansion to ensure compatibility with both RDF/XML and Turtle.

## Enrichment Rules

* Literal content yields semantic individuals
* Attributes may become data properties
* IDs contribute to syntactic identity
* Nested structure forms a compositional tree with semantic properties layered atop

## Codex Agent Expectations

* Maintain structural and semantic dualism
* Support identity resolution: avoid duplicate semantic individuals
* Normalize values safely (punctuation removal, casing)
* Refer to ontology best practices (e.g., use of `rdfs:label`, `owl:Class`)

## Future Work

* Integrate schema vocabulary (e.g., XSD/XPath-driven class hierarchies)
* Emit SHACL/OWL restrictions for known structures
* Add alignment to external vocabularies (e.g., Dublin Core, schema.org)

```
