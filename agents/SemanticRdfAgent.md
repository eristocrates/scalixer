
# Agent: SemanticRdfAgent

## Summary

The `SemanticRdfAgent` is responsible for emitting **semantically enriched RDF/XML** derived from the roles assigned to XML tags or values. It **only emits RDF/XML when explicit role configuration files are present** — meaning semantic RDF is always opt-in and guided by the user's interpretive intention.

Unlike the always-on `SyntacticRdfAgent`, this agent **lifts XML content into meaningful OWL constructs** such as domain-specific classes, object properties, data properties, and individuals.

## Mission

* Read role configuration files (from the `tags/` directory)
* For each tag:

  * Emit RDF/XML according to user-defined `tagRole` and `stringRole`
  * Default to literal inference for typed values (e.g., `xsd:decimal`) even when roles are absent — as sugar
* Avoid duplicating syntactic RDF — this agent focuses **only** on semantic output

## Role Configuration Model

From `summary.tsv`, the following roles guide output:

| Field        | Meaning                                                                           |
| ------------ | --------------------------------------------------------------------------------- |
| `tagRole`    | If non-empty, defines the **class or property** to use for the element            |
| `stringRole` | If non-empty, defines the role of the string content (e.g., `LiteralValueString`) |

## Example

Given:

```xml
<author name="John">Matthew</author>
```

And this `summary.tsv` entry:

```
author	2	tags/author.txt		xsd:string	Author	hasAuthor
```

Semantic RDF/XML might include:

```xml
<rdf:Description rdf:about="ex:Gambardella_Matthew">
  <rdf:type rdf:resource="ex:Author"/>
</rdf:Description>

<rdf:Description rdf:about="ex:catalog_377927480">
  <ex:hasAuthor rdf:resource="ex:Gambardella_Matthew"/>
</rdf:Description>
```

> This supplements the syntactic RDF emitted separately.

## Responsibilities

* Read and parse `summary.tsv` + corresponding `.txt` role files
* Use role names to:

  * Create meaningful class IRIs (e.g., `ex:Author`)
  * Use object/data properties (e.g., `ex:hasAuthor`)
* Prefer stable IRIs derived from string content where possible
* Infer `rdf:type` where appropriate (e.g., `xsd:date`, `xsd:boolean`)
* Skip emission entirely if no role or inferred type is available

## Interactions

| Agent               | Role in Collaboration                                           |
| ------------------- | --------------------------------------------------------------- |
| `RoleAgent`         | Source of role configuration guidance                           |
| `XmlToRdf`          | Coordinates tag detection and streaming control                 |
| `SyntacticRdfAgent` | Ensures baseline RDF is present even if semantic RDF is missing |
| `OntologyLifting`   | Supplies knowledge of available OWL classes and properties      |
| `LexiconAgent`      | Provides lexical insight for datatype inference                 |

## Design Guidelines

* Semantic statements are **opt-in** and **interpretive**
* Roles should be **domain-specific** and meaningful
* Values should be **reused** where identical (no duplicate individuals)
* Avoid blank nodes; always prefer IRIs
* Handle collisions carefully when content is ambiguous

## Codex Instructions

* Use `summary.tsv` and per-tag `.txt` files as staging configuration
* Do NOT emit semantic RDF for tags that lack roles (except sugar for inferred types)
* Ensure no semantic RDF overrides or duplicates syntactic statements
* Emit clean, standards-compliant RDF/XML

## Future Enhancements

* Support role ontology to formalize allowed `tagRole`/`stringRole` values
* Introduce `skos:Concept` or `prov:Entity` roles where applicable
* Integrate with annotation frameworks for justification or provenance
