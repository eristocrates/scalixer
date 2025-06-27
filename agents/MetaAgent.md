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
