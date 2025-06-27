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

## Agents

| Agent Name          | Responsibility                                                                  |
| ------------------- | ------------------------------------------------------------------------------- |
| `XmlToRdf`          | Coordinates XML parsing, lifting, and RDF/XML streaming logic                   |
| `Fs2XmlDoc`         | Documents FS2 + `fs2-data-xml`, advises on parser/stream behavior               |
| `RdfXmlSpec`        | Ensures output matches W3C RDF/XML specification (structure, legality)          |
| `Scala3CompilerDoc` | Confirms idiomatic Scala 3 usage; references compiler when needed               |
| `OntologyLifting`   | Translates XML into OWL semantics; enforces named individual and class patterns |

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

## Future Directions

* Add support for output format fan-out (Turtle, JSON-LD)
* Incorporate SHACL or OWL reasoning checks
* Provide round-trip validation (lower RDF/XML back to XML)
* Integrate Watchman-like file monitoring to stream incoming XML

---

Let me know if you'd like a `DevToolsAgent`, `TestingAgent`, `PerformanceAgent`, or any other supporting role next.
