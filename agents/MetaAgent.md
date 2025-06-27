# MetaAgent

## Responsibility

The `MetaAgent` is responsible for authoring, updating, and maintaining the `.md` definition files of other agents in the `agents/` directory. It acts as a structured intermediary that translates high-level intentions (e.g., “create a new agent that handles test orchestration”) into correctly formatted agent definitions, using a consistent schema.

It enables automation of agent design through structured prompts or Codex-generated transformations.

## Inputs

- Structured agent creation/update requests, in either natural language or formalized schema
- Suggested naming, responsibilities, or modification deltas
- Context from `AGENTS.md` and existing `agents/*.md` files

## Outputs

- New or updated `.md` agent files within `agents/`
- Updates to the central `AGENTS.md` index table

## Behaviors

- Validates agent name uniqueness
- Ensures consistent formatting and schema (e.g., `Responsibility`, `Inputs`, `Outputs`, `Behaviors`)
- Automatically includes agent in `AGENTS.md` unless instructed otherwise
- Can diff previous vs. updated `.md` agents for review

## Implementation Notes

Currently non-executable — serves as a conceptual and documentation agent only. Intended to be paired with Codex or other future automation to perform the actual edits.

In the future, this agent may be backed by a `MetaAgent.scala` or Codex transformation pipeline capable of programmatic AST-level authoring or LLM-reflexive editing.

## Related Agents

- `TestAgent` (which it may help generate)
- `OntologyLifting`, `Fs2XmlDoc` (which it may update based on ontology schema evolution)

## Patch Proposal: Remove Hardcoded exPrefix in XmlToRdf.scala

### Problem

`XmlToRdf.scala` currently embeds the string `"http://example.org/"` directly in IRI
construction logic. While this guarantees expansion for Turtle round-tripping, it
prevents configurable namespaces and hinders reuse across different XML files.

### Solution

Introduce prefix-aware expansion derived from the prefix bindings in `rdfHeader`.
Mapping the `"ex"` prefix to its namespace allows dynamic control and keeps RDF/XML
and Turtle output in sync.

### Implementation Steps

1. **Replace `exPrefix` constant**
   - Remove `val exPrefix = "http://example.org/"`
   - Add a prefix map, for example:
     ```scala
     val prefixMap = Map("ex" -> "http://example.org/")
     ```

2. **Create prefix-aware IRI builder**
   ```scala
   def expandPrefix(prefixedName: String): String = {
     val Array(prefix, local) = prefixedName.split(":", 2)
     prefixMap.getOrElse(prefix, "") + local
   }
   ```

3. **Update IRI construction logic**
   Replace string concatenations with `expandPrefix`, for example:
   ```scala
   expandPrefix(s"ex:${normalizeLiteral(value)}")
   ```

4. **Update `rdfHeader` writer (optional)**
   - Auto-generate `xmlns` declarations from `prefixMap` to avoid duplication.

### Follow-Up

Create regression tests (with `TestAgent`) that:
* Verify IRIs in `rdf:about` and `rdf:resource` are absolute.
* Ensure the `ex:` prefix is declared and round-trips correctly via Turtle.
