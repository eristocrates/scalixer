
# Codex Agents

## Overview

Codex transforms XML into **RDF/XML** using a **modular, agent-based architecture** built with **Scala 3** and **FS2**. All RDF emitted adheres to the **W3C RDF/XML syntax specification**, and RDF output is **streamed**, **staged**, and **semantically enriched** based on detected roles.

### RDF Emission Policy

* **Syntactic RDF/XML** is *always* emitted.
* **Semantic RDF/XML** is emitted *only when roles are staged* using the `roles/` directory.
* Emitted RDF is grouped as:

  * `rdfs:member` → nested XML tags
  * `:attribute` → XML attributes
  * `:xmlString` → element string content
* Attributes are represented with dedicated individuals like `:lang_attribute_1`, typed with `:Lang_Attribute`, containing `:attribute_key` and `:attribute_value`.

---

## Core Agents

### `XmlToRdf`

* **Role**: Top-level orchestrator. Streams XML via FS2 and delegates RDF emission to specialized agents.
* **Output**: Always emits syntactic RDF/XML. Emits semantic RDF/XML if roles are staged.
* **Design Notes**: Stateless; uses `fs2-data-xml`; composable; handles role-driven enrichment dispatch.

---

### `LexiconAgent`

* **Role**: Extracts tag names, attributes, and string content from the XML stream.
* **Output**:

  * Populates `tags/` directory with tag names.
  * Emits `summary.tsv` and `summary.csv-metadata.json`.
* **Functionality**:

  * Infers **XSD primitive types** per tag using lexical analysis.
  * Flags ambiguous or null-tagged fields for human review.
* **Design Notes**: Deterministic; runs early in pipeline to support both semantic lifting and lowering.

---

### `RoleAgent`

* **Role**: Loads role configurations from:

  * `roles/TagRoles/` (e.g. `EntityTag.txt`, `PropertyTag.txt`)
  * `roles/StringRoles/` (e.g. `LabelString.txt`)
* **Functionality**:

  * Determines whether tags or strings are eligible for semantic RDF.
  * Supports **empty file = allow all** semantics.
* **Design Notes**: Stateless; reliable signal layer for `SemanticRdfAgent`.

---

### `SyntacticRdfAgent`

* **Role**: Emits RDF/XML that mirrors XML structure using:

  * `rdfs:member` → tag nesting
  * `:attribute` → XML attribute values
  * `:xmlString` → element text content
* **Naming Patterns**:

  * `<tag>_tag_<N>` → tag-level subject
  * `<tag>_string_<N>` → content string
  * `<key>_attribute_<N>` → attribute value as node
* **Output Example**:

  ```
  :author_tag_1 a :Author_Tag ;
    :attribute :lang_attribute_1 ;
    :xmlString "Gambardella" .

  :lang_attribute_1 a :Lang_Attribute ;
    :attribute_key "lang" ;
    :attribute_value "en" .
  ```
* **Design Notes**: Always runs; emits only syntactic structure; infers no roles.

---

### `SemanticRdfAgent`

* **Role**: Emits RDF/XML *only when roles apply*.
* **Functionality**:

  * Uses `RoleAgent` to check if tag or string is semantically staged.
  * Attaches semantic types (e.g., `:Table`, `:Author`) and relationships (e.g., `:hasName`, `rdfs:label`).
* **Design Notes**:

  * Composable: merges with `SyntacticRdfAgent` output.
  * Role-driven: safe to run without staging (no output).

---

### `CsvwAgent`

* **Role**: Writes `summary.csvw` to describe the schema of inferred tags.
* **Functionality**:

  * Summarizes each tag’s:

    * Inferred datatype
    * Detected roles
    * Tag frequency
  * Supports downstream tools for validation, visualization, or pipeline transformation.
* **Design Notes**: Terminal agent; runs after `LexiconAgent` and `RoleAgent`.

---

## Support & Meta Agents

### `Fs2XmlDoc`

* **Role**: Documents idioms and streaming behaviors from `fs2-data-xml`.
* **Content**:

  * Event-based node representation
  * Stream combinator patterns
  * Error handling idioms
* **Design Notes**: Not executable—supports code clarity and idiomatic Scala practices.

---

### `RdfXmlSpec`

* **Role**: Validates RDF/XML output against [W3C RDF/XML Syntax Specification](https://www.w3.org/TR/rdf-syntax-grammar/).
* **Functionality**:

  * Ensures:

    * Valid namespaces and qnames
    * Proper nesting of `rdf:Description`
    * Legal use of `rdf:parseType`, `rdf:resource`, etc.
* **Design Notes**: May operate as unit test or CI rule.

---

### `Scala3CompilerDoc`

* **Role**: Advises best practices for Scala 3 including:

  * Enums
  * Givens
  * Extension methods
* **Purpose**: Ensures idiomatic, maintainable Scala is used throughout pipeline.

---

### `TestAgent`

* **Role**: Auto-generates MUnit test suites.
* **Functionality**:

  * Validates RDF conformance
  * Ensures semantic roles behave as expected
  * Confirms CSV-W summaries match tag inferences
* **Design Notes**: Deterministic tests can be regenerated as data/roles evolve.

---

### `MetaAgent`

* **Role**: Maintains all agent `.md` files, validates structural consistency, and enforces naming conventions.
* **Functionality**:

  * Generates and synchronizes entries in `AGENTS.md`
  * Audits role files, doc headings, and responsibility statements
* **Design Notes**: Internal tool for agent system governance.

---

## Workflow Overview

```text
INPUT XML
   ↓ (stream)
LexiconAgent
   ↓
RoleAgent
   ↓
SyntacticRdfAgent
   ↓
SemanticRdfAgent
   ↓
CsvwAgent
   ↓
RdfXmlSpec + TestAgent
```

---

## Future Planning

* **Lowering Profiles**: Semantic metadata may carry an identifier for source XML tag model (e.g. ArcGIS, MusicXML).
* **Lifting Profiles**: Attach "profile hints" during lifting that describe which downstream XML styles the data is compatible with.
* **Transformation Matrix**: `ProfileAgent` may one day infer abstract models across heterogeneous XML types.
