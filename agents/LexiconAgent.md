
# Agent: LexiconAgent

## Summary

The `LexiconAgent` is responsible for managing the **summary.tsv lexicon file** that captures essential metadata about every XML tag encountered during stream processing. This metadata includes inferred datatypes, tag and string roles, and reference paths for each tag’s extracted values.

The lexicon serves as a **staging layer** for semantic enrichment — acting as a semantic scaffolding from which semantic RDF can be selectively enabled based on end-user intervention. While syntactic RDF is always emitted, semantic RDF is conditionally triggered based on the lexicon’s declared roles.

## Mission

Construct and maintain a lexicon-driven semantic configuration system by:

* Tracking all distinct XML tag names observed
* Recording example values to infer XSD primitive types
* Emitting `summary.tsv` with structure-aware metadata
* Generating placeholder `.txt` files for `tagRole` and `stringRole` per tag
* Supporting semantic lifting logic by encoding per-tag intent and datatype clues

## Fields in `summary.tsv`

| Field              | Description                                                              |
| ------------------ | ------------------------------------------------------------------------ |
| `tag`              | The XML tag name                                                         |
| `count`            | Number of distinct string values observed                                |
| `filename`         | Path to extracted values (e.g., `tags/Author.txt`)                       |
| `inferredDatatype` | XSD type inferred from lexical pattern (e.g., `xsd:boolean`, `xsd:date`) |
| `tagRole`          | Indicates tag-level RDF interpretation (e.g., `PropertyTag`, `ClassTag`) |
| `stringRole`       | Indicates string-level RDF interpretation (e.g., `LiteralValueString`)   |

## Lexicon Behavior

* All tags always get a `tagRole.txt` file (initially empty)
* All tags with string content get a `stringRole.txt` file (initially empty)
* If the inferred datatype is not `xsd:string`, default the `stringRole` to `LiteralValueString`
* The presence of role files triggers semantic RDF output; absence or emptiness preserves syntactic-only output

## Responsibilities

* Emit `tags/tags.txt` listing all discovered tags
* Emit `tags/*.txt` for each tag’s values
* Emit `tags/tagRole/*.txt` and `tags/stringRole/*.txt` placeholders for user input
* Maintain `summary.tsv` as a complete semantic configuration manifest

## Codex Instructions

* When extending `runInferAndLexicon`, never emit RDF — just lexicon metadata
* Treat all XML attributes as if they were namespaced tags prefixed with `@` (e.g., `@lang`)
* Infer datatypes using high-confidence regex patterns:

  * `xsd:boolean`, `xsd:decimal`, `xsd:double`, `xsd:date`, `xsd:gYearMonth`, `xsd:gYear`, `xsd:dateTime`
* Exclude complex types (e.g., `QName`, `NOTATION`) for now
* Do not emit summary lines for tags with no values

## Interactions with Other Agents

* `RoleAgent`: Relies on lexicon role files to decide when semantic RDF should be emitted
* `OntologyLifting`: Uses the lexicon metadata to drive semantic individual creation and type inference
* `SyntacticRdfAgent`: Ignores roles but uses tag presence to emit default RDF
* `SemanticRdfAgent`: Activated only when roles are declared in lexicon
* `CsvwAgent`: Will use the lexicon to annotate CSVW metadata files for downstream tooling

## Future Directions

* Allow user overrides in lexicon (e.g., force datatype, override role)
* Promote summary.tsv to RDF metadata format (e.g., CSVW, SHACL shape manifest)
* Annotate lexical values with provenance (e.g., original line/element in XML stream)
* Auto-suggest `tagRole` and `stringRole` via heuristic classification
