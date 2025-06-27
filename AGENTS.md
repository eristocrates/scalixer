Perfect. Here's a rewritten **Core Agent** file (formerly `agents.md`) that serves as the **narrative through line** and coordination point between all other agents. It summarizes the entire project vision and provides the Codex system with top-level orientation.

---

# Agent: CoreProject

## Summary

This project transforms XML documents into **RDF/XML output** using a **fully streaming, pure Scala 3 pipeline**. The goal is to semantically enrich XML structures into meaningful OWL ontologies, emitting standards-compliant RDF/XML with zero Java interop or black-box frameworks.

The agent provides a **central narrative** to align all Codex sub-agents, and serves as the entrypoint for reasoning about the overall architecture, design intentions, and best practices.

## Mission

Create a clean, declarative transformation pipeline that:

* Parses XML using `fs2-data-xml`
* Semantically lifts XML elements into OWL constructs
* Serializes triples into **valid RDF/XML** per [W3C RDF/XML Syntax Spec]
* Leverages **Scala 3 idioms** and **FS2 streaming**
* Avoids Java frameworks like Jena or Banana-RDF

[W3C RDF/XML Syntax Spec]: ./src/main/resources/rdf-1.1-XML-Syntax.html

## Architecture Overview

```
[XML Source]
    ↓
fs2-data-xml (streaming parser)
    ↓
Semantic Lifting Rules (OntologyLifting agent)
    ↓
RDF Triple Stream (domain-aware, structured)
    ↓
RDF/XML Serializer (RdfXmlSpec agent)
    ↓
[RDF/XML Output File]
```

## Codebase Structure

| File / Folder                                | Role                                                   |
| -------------------------------------------- | ------------------------------------------------------ |
| `XmlToRdf.scala`                             | Main transformation logic                              |
| `example.xml`                                | Sample input XML                                       |
| `example.rdf`                                | RDF/XML output file                                    |
| `src/main/resources/rdf-1.1-XML-Syntax.html` | Canonical RDF/XML format reference                     |
| `src/main/resources/fs2-data-docs-*`         | JavaDoc mirror of `fs2-data-xml` for streaming details |
| `src/main/resources/scala3/docs/_docs`       | Scala 3 language and compiler documentation            |

## Environment: Offline Java + SBT Configuration

This project includes a fully offline development environment using pre-bundled binaries:

- **Java**: located at `tools/jdk-17.0.15.6-hotspot/bin/java.exe`
- **SBT Launcher**: `tools/sbt-launch/bin/sbt-launch.jar`

The `setup.sh` script ensures these tools are made available in `PATH`:

```bash
export JAVA_HOME="$PWD/tools/jdk-17.0.15.6-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"

# Wraps sbt-launch.jar in a shell script:
export PATH="$PWD/tools/sbt-launch/bin:$PATH"
## Agents

| Agent Name          | Responsibility                                                                  |
| ------------------- | ------------------------------------------------------------------------------- |
| `XmlToRdf`          | Coordinates XML parsing, lifting, and RDF/XML streaming logic                   |
| `Fs2XmlDoc`         | Documents FS2 + `fs2-data-xml`, advises on parser/stream behavior               |
| `RdfXmlSpec`        | Ensures output matches W3C RDF/XML specification (structure, legality)          |
| `Scala3CompilerDoc` | Confirms idiomatic Scala 3 usage; references compiler when needed               |
| `OntologyLifting`   | Translates XML into OWL semantics; enforces named individual and class patterns |
| `TestAgent`         | Generates MUnit-based tests to validate RDF/XML conformance and semantic lifting|

Each agent maintains its own file (e.g., `OntologyLifting.md`), allowing Codex to specialize its behavior based on the active task.

## Design Goals

* **Streaming**: All logic uses FS2 streams — no intermediate data structures
* **Semantics-First**: Treat XML as a source of semantic meaning, not just structure
* **Spec Compliance**: Output RDF/XML must validate against W3C requirements
* **Scala-Only**: No Java dependencies; fully idiomatic Scala 3
* **Extensibility**: Future targets include JSON-LD, Turtle output — but via separate steps

## Codex Instructions

When editing or improving this project:

1. **Always honor the W3C RDF/XML syntax specification**
2. **Consult the lifting agent** before flattening or normalizing nested XML
3. **Use only streaming logic**; never introduce non-streaming collection-based code
4. **Use Scala 3 idioms** and features (e.g., `using`, `given`, for-comprehensions)
5. **Defer to specialized agents** as needed — this file defines the architecture, not the implementation specifics

## Testing Philosophy

- There is **no need to manually write tests** — Codex is encouraged to **generate them**.
- Tests should be created for new functionality or to verify specification adherence.
- Generated tests go under: `src/test/scala/`
- Use: MUnit (`org.scalameta %% munit % 1.0.0`) as the test framework.
- Emphasize tests that:
  - Confirm RDF/XML syntax is valid
  - Assert that RDF match expected outputs
  - Validate semantic lifting behavior (e.g., class inference, rdfs:member, property generation)

The Codex agent should **assume the role of a semantic quality validator**, ensuring that each RDF/XML output structurally and semantically matches the design intent.

## Future Directions

* Add support for output format fan-out (Turtle, JSON-LD)
* Incorporate SHACL or OWL reasoning checks
* Provide round-trip validation (lower RDF/XML back to XML)
* Integrate Watchman-like file monitoring to stream incoming XML
