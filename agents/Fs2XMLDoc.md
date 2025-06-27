# Agent: Fs2XmlDoc

## Summary

This agent is responsible for understanding and leveraging the `fs2-data-xml` streaming library for parsing XML in a purely functional and idiomatic Scala 3 environment.

## Local Documentation Reference

The full offline documentation for `fs2-data-xml` is stored at:

```
src/main/resources/fs2-data-docs-2.13-1.12.0/
```

This includes the full scaladoc HTML mirror for the version `1.12.0` of the `fs2-data-xml` module compiled against Scala 2.13 (still valid for syntax and structure).

## Library Role

`fs2-data-xml` is used to:

* Parse XML streams from `InputStream`, `Path`, or `String`
* Represent XML structure via `XmlEvent` and associated types
* Stream parsed XML into downstream functional pipelines
* Allow seamless composition with `fs2.Stream[F, A]` patterns

This agent enables Codex to:

* Understand the XML event model used in `XmlToRdf.scala`
* Use constructors like `StartTag`, `EndTag`, `Text`, `EmptyTag`, `EntityRef`, etc.
* Navigate namespaces, attributes, and hierarchical structure via event sequences

## Canonical Types

Key types Codex should reference:

* `fs2.data.xml.XmlEvent`
* `fs2.data.xml.XmlEvent.StartTag`
* `fs2.data.xml.XmlEvent.Text`
* `fs2.data.xml.XmlEvent.EndTag`
* `fs2.data.xml.XmlParser`
* `fs2.data.xml.XmlSettings`

These types are used in pattern matching and stream manipulation logic in `XmlToRdf.scala`.

## Example Flow

```scala
Files[IO]
  .readAll(xmlInputPath)
  .through(fs2.text.utf8.decode)
  .through(events[IO](XmlSettings.default))
  .through(transformToRdf)
  .through(fs2.text.utf8.encode)
  .through(Files[IO].writeAll(rdfOutputPath))
```

Codex should understand that:

* Each `XmlEvent` is processed functionally, with no mutation
* State must be threaded using `fs2.Pull`, `scan`, or `flatMapAccumulate`
* Attributes are accessed from `StartTag.attributes` and fully qualified names are resolved with `QName`

## Goals

The agent supports:

* Validating or suggesting improvements to XML streaming logic
* Emitting RDF/XML elements in response to encountered XML events
* Performing incremental lifting (e.g., matching `StartTag("book")` → `rdf:Description` of type `:Book`)
* Debugging issues with event ordering, namespace leakage, or content types

## Integration

This agent supports:

* `XmlToRdf` — as the consumer of parsed `XmlEvent` streams
* `RdfXmlSpec` — by framing which XML events trigger specific RDF/XML constructs
* `Scala3CompilerDoc` — ensuring idiomatic, type-safe streaming logic is used

## Codex Instructions

* Prefer working with XML as a stream of `XmlEvent`s, not as a DOM
* Use `StartTag`, `Text`, and `EndTag` to construct rdf/xml statements dynamically
* Pay special attention to namespace resolution and qualified names
* Respect ordering constraints (e.g., attributes must be present before content)
* Do not buffer the entire XML document in memory unless explicitly requested

