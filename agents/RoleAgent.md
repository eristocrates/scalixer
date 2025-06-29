# Agent: RoleAgent

## Summary

The `RoleAgent` governs the interpretation of lexicon-defined `tagRole` and `stringRole` values to determine whether **semantic RDF/XML statements** should be emitted during transformation. It operates as a **semantic dispatch controller**, consulting role configuration files produced by the `LexiconAgent` to orchestrate conditional semantic enrichment during RDF generation.

## Mission

* Load all available `tagRole/*.txt` and `stringRole/*.txt` files
* For each observed tag, determine:

  * Whether semantic RDF should be generated
  * Which OWL construct(s) or RDF patterns should be applied
* Ensure **safety by default**: If no roles are defined, only syntactic RDF is emitted

## Design Principles

* **Declarative control**: User configures roles through editable text files
* **Syntactic defaults**: RDF is always emitted for syntactic structure regardless of roles
* **Semantic opt-in**: RDF with OWL or RDFS semantics is only emitted if a role is defined
* **Config-first enrichment**: Roles act as semantic triggers, not embedded logic

## Role Definitions

| Role Type    | Sample Values                                          | Effect                                                                 |
| ------------ | ------------------------------------------------------ | ---------------------------------------------------------------------- |
| `tagRole`    | `ClassTag`, `PropertyTag`, `IndividualTag`             | Controls whether the tag emits OWL classes, properties, or individuals |
| `stringRole` | `LiteralValueString`, `Identifier`, `Label`, `Comment` | Controls how string content becomes RDF literals or semantic links     |

### Role File Locations

* `tags/tagRole/<tag>.txt`: Defines the RDF role of the tag
* `tags/stringRole/<tag>.txt`: Defines how to interpret text content in RDF

These are parsed as plaintext files — usually containing a single line, e.g.:

```txt
ClassTag
```

## Responsibilities

* Provide a pure function:

  ```scala
  def getTagRole(tag: String): Option[String]
  def getStringRole(tag: String): Option[String]
  ```
* Cache role definitions during stream startup
* Flag tags with semantic roles so that the `SemanticRdfAgent` can act on them
* Report unresolved roles if `strictMode` is enabled

## Codex Instructions

* Always defer to the presence and content of role files before generating semantic RDF
* If `tagRole.txt` or `stringRole.txt` is missing or empty:

  * Emit **only syntactic RDF**
* If a role is defined:

  * Provide guidance to `SemanticRdfAgent` about the appropriate enrichment strategy
* Roles must be loaded once, not repeatedly during the stream

## Interactions with Other Agents

* `LexiconAgent`: Supplies the `.txt` role files and inferred defaults
* `SemanticRdfAgent`: Relies on the decision of `RoleAgent` to emit RDF types, literals, labels, etc.
* `SyntacticRdfAgent`: Ignores roles, but requires awareness to avoid duplication

## Future Directions

* Support multi-role declarations (e.g., `ClassTag, IndividualTag`)
* Add `DefaultRoleRules.scala` to heuristically guess roles if unspecified
* Enable warnings for mismatches between datatype and role (e.g., `xsd:boolean` with `Label`)
* Support role ontology vocabularies (e.g., use RDF vocabulary for roles)

