# agents.md

## Project Description

This project streams XML using FS2, parses with fs2-data-xml, and emits RDF/XML. We are using pure Scala 3 and avoiding Jena. The RDF/XML is streamed directly from XML elements via functional transformation logic.
## Guidance Sources

- When generating or rewriting RDF/XML output, follow the W3C RDF/XML Syntax Specification located at:
  
  `src/main/resources/rdf-1.1-XML-Syntax.html`

  Treat this document as the normative reference for correct RDF/XML formatting.

- The agent should ensure that any serialization of RDF conforms to this specification, especially when modifying `XmlToRdf.scala` or any related serialization logic.

- Do **not** use heuristics or undocumented formats; rely only on the structure and constraints defined in this RDF/XML spec.

## XmlToRdf.scala Agent Behavior

- Your goal is to produce RDF/XML output that fully conforms to the W3C specification (see reference above).
- When unsure how to serialize an element or triple, consult `rdf-1.1-XML-Syntax.html` first.

## Goals

- Stay within Scala 3 (no Java interop or reflection-based libs).
- Stream RDF/XML using `fs2.Stream`.
- Use `fs2-data-xml` for parsing, emitting RDF triples directly.
- Later: extend output formats (Turtle, JSON-LD) as a separate pipeline step.

## What Codex Should Know

- Do not introduce Jena or Banana-RDF.
- FS2 and `fs2-data-xml` are the only streaming libraries in use.
- Assume output is a side-effecting stream written to file.

## Current Working File

- `XmlToRdf.scala`: main entrypoint.
- `example.xml`: sample XML input.
- Output: `example.rdf` (RDF/XML serialized from XML).

## Assistance I Want

- Optimize streaming logic.
- Extend how attributes or nested text nodes are handled.
- Debug or profile bottlenecks (if any).
- Structure streaming pipelines functionally and efficiently.

