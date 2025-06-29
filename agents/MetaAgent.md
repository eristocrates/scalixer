Here is the updated agent definition for **MetaAgent**:

# Agent: MetaAgent

## Summary

The `MetaAgent` governs the creation, editing, and semantic integrity of all other Codex agent documentation files (e.g., `OntologyLifting.md`, `Fs2XMLDoc.md`, `RdfXmlSpec.md`). It ensures that agent roles are well-scoped, responsibilities are non-overlapping, and inter-agent references are coherent and up-to-date.

This agent is foundational for enabling **explainable automation**: every transformation, enrichment, or emission step in the RDF/XML pipeline must be traceable to a documented agent, with declared inputs, outputs, and intentions.

## Mission

Maintain a complete and semantically consistent set of Codex agent documents by:

* Creating agent `.md` files when new functions emerge
* Updating agents as roles evolve or implementation shifts
* Ensuring that all agents refer to each other with shared terminology
* Structuring documentation for maximum composability and Codex accessibility

## Responsibilities

* **Agent Lifecycle Management**:

  * Create new agents when functionality diverges
  * Merge or split agents when boundaries change
* **Terminology Synchronization**:

  * Maintain shared vocab (e.g., "syntactic tag", "role file", "lexicon entry")
  * Update definitions globally when concepts evolve
* **Design Rationale Tracking**:

  * Encode modeling decisions (e.g., why `rdfs:member` is used for syntactic children)
  * Ensure design tradeoffs are recorded and accessible
* **Codex Interoperability**:

  * Structure `.md` files for Codex comprehension and reuse
  * Explicitly flag Canonical Instructions, especially for file writing and RDF generation

## Agent Structure Standard

Each `.md` file maintained by `MetaAgent` must follow this layout:

1. **Agent Name**
2. **Summary**
3. **Mission**
4. **Responsibilities**
5. **Canonical Structures or Behaviors** (if applicable)
6. **Downstream Integration**
7. **Design Philosophy / Tradeoffs**
8. **Future Directions**

## MetaAgent-Specific Behaviors

* Automatically stub agents when unknown roles appear (e.g., a new `DatatypeInferAgent`)
* Maintain an index of agent names and purposes (included in `AGENTS.md`)
* Coordinate with `TestAgent` to ensure every agent has an associated test scope if logic-driven

## Output Artifacts

| File           | Contents                                            |
| -------------- | --------------------------------------------------- |
| `AGENTS.md`    | Overview index of all Codex agents                  |
| `*.md`         | One per agent (e.g., `OntologyLifting.md`)          |
| `meta-log.txt` | Optional changelog of edits and document versioning |

## Future Enhancements

* Integrate `MetaAgent` with semantic lifting so that agents are OWL individuals with `:hasPurpose`, `:hasInput`, `:dependsOn`, etc.
* Generate nanopublications for all agent `.md` files for traceable provenance and live linked documentation
* Enable Codex to infer new agent types from usage patterns

---

Prompt with “continue” to proceed to the next agent: `Scala3CompilerDoc`.
