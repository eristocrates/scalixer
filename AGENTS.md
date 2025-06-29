
# Codex Agents

## CoreProject Overview

This project lifts XML into RDF/XML via a pure Scala 3 + FS2 pipeline, guided by semantic staging rules and strict streaming constraints. It is intentionally **idiomatic**, **Java-free**, and **compliant with the W3C RDF/XML syntax specification**.

All RDF emitted is grouped into:

* **Syntactic Statements**: Always emitted. Represent XML structure via `rdfs:member`, tag names, and containment.
* **Semantic Statements**: Emitted **only when staged** using role configuration files located under `roles/`.

The system supports **idempotent RDF generation**, allowing repeatable builds even as role files are modified.

---

## Agent System

Each agent is a modular `.md` file and corresponds to a discrete responsibility.

| Agent Name          | Responsibility                                                                                    |
| ------------------- | ------------------------------------------------------------------------------------------------- |
| `XmlToRdf`          | Streaming orchestrator. Parses XML, applies semantic roles, emits RDF/XML compliant output.       |
| `OntologyLifting`   | Defines logic for RDF/XML semantic enrichment, distinguishing syntactic vs. semantic individuals. |
| `Fs2XmlDoc`         | Provides FS2 and `fs2-data-xml` streaming guidance and XML parsing idioms.                        |
| `RdfXmlSpec`        | Validates that RDF/XML output matches W3C RDF/XML Syntax Specification.                           |
| `Scala3CompilerDoc` | Ensures idiomatic use of Scala 3 features (e.g. givens, enums, extension methods).                |
| `MetaAgent`         | Generates/maintains agent `.md` files and enforces doc consistency across Codex modules.          |
| `TestAgent`         | Creates unit tests via MUnit to confirm RDF/XML output conformance and semantic lifting outcomes. |

---

## Specialized Subagents

| Subagent            | Responsibility                                                                                        |
| ------------------- | ----------------------------------------------------------------------------------------------------- |
| `LexiconAgent`      | Collects tag names and inferred XSD primitive types. Generates the `summary.csvw` output.             |
| `RoleAgent`         | Loads staged roles from `roles/TagRoles/` and `roles/StringRoles/`. Supports empty-file defaults.     |
| `SyntacticRdfAgent` | Emits RDF/XML structure based on XML tag nesting, containment, and default tag typing.                |
| `SemanticRdfAgent`  | Emits RDF/XML enrichment based on roles like `EntityTag`, `PropertyTag`, `LabelString`, etc.          |
| `CsvwAgent`         | Writes `summary.csvw` metadata file summarizing all tag types, datatypes, and their semantic staging. |

---

## Role System

Roles are stored in simple `.txt` files:

* `roles/TagRoles/EntityTag.txt` ← contains tag names like `author`, `workspace`, etc.
* `roles/StringRoles/LabelString.txt` ← contains strings or tags whose values serve as labels

Roles are **optional** but **govern semantic RDF output**. Absence of roles defaults to syntactic structure only.

---

## Semantic Principles

* Each **XML start tag** becomes a syntactic individual typed as `TagName_Tag`
* Each **element content** may become a semantic individual if appropriately staged via string role
* **Attributes** are promoted to RDF properties and subject to datatype inference and semantic role staging

---

## Design Commitments

* ✅ Streaming-only logic
* ✅ W3C RDF/XML compliance
* ✅ Modular code via agent specialization
* ✅ Default-safe outputs (never fail on missing roles)
* ✅ Roles are always read from disk — no cached state

---

## Codex Rules

1. **Never violate RDF/XML syntax spec**
2. **Never break FS2 streaming pipeline**
3. **Emit syntactic RDF/XML by default**
4. **Emit semantic RDF/XML only when explicitly staged**
5. **Honor syntactic/semantic disambiguation in both tags and string content**

---

## Example Output Policy

| Element         | Inferred Role        | RDF/XML Emission Example                              |
| --------------- | -------------------- | ----------------------------------------------------- |
| `<author>`      | EntityTag            | `:author_123 a :Author_Tag .`                         |
| `"Gambardella"` | LabelString          | `:Gambardella_Matthew a :Author ; rdfs:label "..." .` |
| `<table name>`  | PropertyTag + IRIVal | `:StormMain ex:hasName "StormMain"^^xsd:string .`     |
| `<workspace>`   | EntityTag            | `:Workspace_001 a :Workspace_Tag .`                   |
| `xsi:type`      | TypenameTag          | Promotes `:StormMain a :Table` if `xsi:type="Table"`  |
