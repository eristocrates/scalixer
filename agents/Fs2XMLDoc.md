Here is the updated agent definition for **Fs2XMLDoc**:

# Agent: Fs2XMLDoc

## Summary

The `Fs2XMLDoc` agent documents and governs the use of the [`fs2-data-xml`](https://github.com/satabin/fs2-data) streaming parser within the RDF/XML transformation pipeline. It ensures correct usage of FS2 stream combinators, accurate event handling (`StartTag`, `EndTag`, `Text`, etc.), and preserves the structure and semantics of the original XML during parsing.

This agent exists to maintain streaming purity while maximizing information fidelity for subsequent semantic enrichment steps, especially during ontology lifting.

## Mission

Guarantee that XML is parsed as a **semantically interpretable stream of XML Infoset–inspired events**, and ensure full compatibility with:

* Syntactic RDF emission via the `SyntacticRdfAgent`
* Semantic enrichment via the `OntologyLifting` and `SemanticRdfAgent`
* Roles and lexicon mappings from `RoleAgent` and `LexiconAgent`

## Responsibilities

* Interpret each `XmlEvent` with precise structural awareness
* Maintain correct hierarchical context using a stack or fold pattern
* Extract:

  * Element tags
  * Attributes
  * Text content
* Preserve the order and nesting for downstream RDF/XML generation
* Flag and gracefully skip unsupported events (e.g., `XmlDecl`, `Comment`)

## Canonical FS2 Event Types

| XML Event Type                                | Usage Notes                                                          |
| --------------------------------------------- | -------------------------------------------------------------------- |
| `StartTag`                                    | Push tag to stack, identify syntactic parent, initialize text buffer |
| `EndTag`                                      | Pop tag from stack, flush buffered content if present                |
| `Text` / `XmlString`                          | Buffer trimmed text content for potential string role inference      |
| `Attribute` (via `StartTag`)                  | Collected per element; passed to lexicon and role inference          |
| `XmlDecl`, `Comment`, `ProcessingInstruction` | Ignored unless tagged for provenance in future work                  |

## Design Principles

* **Streaming purity**: Avoid building trees; consume the stream in-place
* **Order preservation**: Emit RDF/XML in the same order as the original XML structure
* **Stateful parsing**: Use a maintained stack (`List[String]`) to track depth and ancestry
* **Annotation preservation**: Allow later agents to enrich parsed tags based on collected roles, tags, and inferred semantics

## Downstream Integration

The `Fs2XMLDoc` agent provides essential inputs to:

* `LexiconAgent`: collects candidate strings per tag for role classification
* `RoleAgent`: maps tags to syntactic/semantic roles
* `SyntacticRdfAgent`: emits `rdfs:member` and OWL class declarations based on hierarchy
* `SemanticRdfAgent`: emits `ex:hasX`-style enriched relations based on roles

## Future Enhancements

* Provide full Infoset logging when enabled (e.g., to `diagnostic/infoset.log`)
* Add XInclude resolution support
* Create an override mode to extract non-hierarchical views of documents (e.g., flat metadata scans)
* Facilitate diagnostic view pipelines (e.g., stream visualizations of event flows)

---

Prompt with “continue” to proceed to the next agent: `MetaAgent`.
