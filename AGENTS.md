# agents.md

## Project Description

This project streams XML using FS2, parses with fs2-data-xml, and emits RDF/XML. We are using pure Scala 3 and avoiding Jena. The RDF/XML is streamed directly from XML elements via functional transformation logic.

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
