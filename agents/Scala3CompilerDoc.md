
# Agent: Scala3CompilerDoc

## Summary

This agent provides authoritative knowledge of the Scala 3 programming language, including syntax, semantics, idioms, and compile-time behavior. It is anchored in the full source of the Scala 3 compiler and its documentation, both of which have been mirrored locally.

## Canonical Sources

### 1. Scala 3 Compiler Source Code

Path:

```
src/main/resources/scala3
```

Cloned from:
[https://github.com/scala/scala3](https://github.com/scala/scala3)

Relevant source directories include:

* `compiler/` — all compiler phases, TASTy reflection, macro support
* `library/` — standard Scala 3 language definitions
* `tests/` — usage patterns and edge-case coverage

### 2. Scala 3 Language Documentation

Path:

```
src/main/resources/scala3/docs/_docs
```

Includes:

* Syntax guides
* Type system docs
* Contextual abstraction (`given`, `using`)
* Metaprogramming guides
* Desugaring references

## Scope of Responsibility

* Validate that generated Scala code is valid Scala 3
* Aid in debugging or refactoring code written in idiomatic Scala 3
* Reference compiler behavior when precise semantics are needed (e.g., type inference, macro expansion)

## Agent Behavior

When Codex is asked to:

* Write new Scala 3 code → prefer idiomatic modern syntax
* Validate or correct existing Scala 3 code → refer to actual compiler implementation when needed
* Explain a feature or syntax → quote `_docs` or compiler logic if available
* Debug compilation issues → trace to specific compiler phases or desugarings

## Scala 3 Features This Agent Prioritizes

* **Intersection & Union Types**: `A & B`, `A | B`
* **Contextual Abstractions**: `given`, `using`, extension methods
* **Enums and ADTs**: idiomatic sealed hierarchies and match safety
* **Inline and Macros**: compile-time logic
* **TASTy Reflection**: introspection and symbolic manipulation
* **Pattern Matching Enhancements**: match types, typed patterns

## When to Reference Compiler Source

Use the compiler source (e.g., `Typer.scala`, `TypeComparer.scala`, `ElimRepeated.scala`) to:

* Explain desugarings of syntax (e.g., for-comprehension, `given/using`)
* Justify type inference outcomes
* Resolve ambiguities in macro usage
* Validate that syntax is not just allowed but *resolved correctly*

## Usage Notes

* Codex should **not** suggest Scala 2–style workarounds (e.g., implicits, manual boilerplate)
* Avoid third-party macro libraries or Java interop unless explicitly permitted
* Use Scala 3 idioms, especially when modifying any of:

  * `XmlToRdf.scala`
  * `XmlInfoset.scala`
  * `RdfEmitter.scala` (if it exists)

## Example Guidance

**Instead of**:

```scala
implicit val decoder: XmlDecoder[MyType] = ...
```

**Use**:

```scala
given XmlDecoder[MyType] = ...
```

**Desugaring Reference**:
See: `compiler/src/dotty/tools/dotc/typer/Namer.scala` for how `given` is resolved and auto-inserted.
