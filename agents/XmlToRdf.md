
### ✅ `XmlToRdf.md`

```markdown
# Agent: XmlToRdf

## Summary

This agent transforms XML documents into RDF/XML using FS2 and `fs2-data-xml` in pure Scala 3. Its core responsibility is to stream `XmlEvent`s and produce RDF/XML output conforming to the W3C RDF/XML syntax specification.

## Core File

* `src/main/scala/XmlToRdf.scala`: Main transformation logic
* Input: `example.xml`
* Output: `example.rdf`

## Output Format Specification

All RDF/XML output **must** conform to the W3C RDF/XML specification:

```

src/main/resources/rdf-1.1-XML-Syntax.html

````

No output should violate its grammar or structural rules.

## Input Semantics

The input XML is treated as a syntactic container for semantic content.

Example:

```xml
<book id="bk101">
  <author>Gambardella, Matthew</author>
</book>
````

### RDF Interpretation:

* `book` → OWL `Class` (semantic) and `Book_Tag` (syntactic)
* `id="bk101"` → Named syntactic individual `:book_...`
* `"Gambardella, Matthew"` → Named semantic individual `:Gambardella_Matthew`
* A separate syntactic tag-based node like `:author_...` is created
* rdfs\:member connects syntactic nodes; `hasX` properties link to semantic values

## Semantic Lifting Guidelines

### 1. **Dual Individual Modeling**

* **Syntactic individuals** represent XML tag instances.

  * Named as `<tag>_<hash>`, e.g. `author_1234`
  * Typed as `<Tag>_Tag`, e.g. `Author_Tag`

* **Semantic individuals** represent meaningful values.

  * Normalized from text content (e.g., `Gambardella, Matthew` → `Gambardella_Matthew`)
  * Typed as `<Tag>`, e.g. `Author`

### 2. **Class Declaration**

* For every element tag `X`, generate:

  * `:X_Tag` — OWL class for the syntactic node
  * `:X` — OWL class for the semantic content

### 3. **rdfs\:member vs. hasX Distinction**

* Use `rdfs:member` for linking to syntactic tag instances
* Use `ex:hasX` for linking to normalized semantic individuals

Example:

```xml
<author>Gambardella, Matthew</author>
```

Becomes:

```xml
<rdf:Description rdf:about=".../book_...">
  <rdfs:member rdf:resource=".../author_1234" />
  <ex:hasAuthor rdf:resource=".../Gambardella_Matthew" />
</rdf:Description>
```

### 4. **Normalization**

* Semantic individual IRIs normalize:

  * Remove punctuation
  * Collapse whitespace to `_`
  * PascalCase or Title\_Snake\_Case as needed for readability
* Tag IRIs always keep the tag name and a hash (for uniqueness)

### 5. **RDF/XML Emission**

* Emit all triples as legal RDF/XML `<rdf:Description>` blocks
* Maintain proper nesting and scoping
* Prefixes:

  * `rdf:`, `rdfs:`, `owl:`, and `ex:` must always be declared

## Agent Behavior

* Stream-based — no buffering or tree-building
* Matches on `XmlEvent`s and emits RDF/XML strings as `Stream[IO, Byte]`
* Avoid blocking or imperative IO
* Composable functional logic

## Codex Agent Expectations

* Validate output against RDF/XML spec
* Reference `Fs2XmlDoc.md` for parser behavior
* Refactor emission logic for reuse across individuals and classes
* Fully respect prefix expansion in all IRIs

## Future Goals

* Turtle / JSON-LD output streaming via same pipeline
* Automatically emit OWL `Class` declarations for all classes
* Integrate schema-driven enrichments (e.g., OWL cardinality)
* Automate rdfs\:label inclusion with fallback language tags

````
