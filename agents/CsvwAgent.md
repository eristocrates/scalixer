
# Agent: CsvwAgent

## Summary

The `CsvwAgent` is responsible for producing a **CSVW-style summary output** (`summary.tsv`) that describes all observed XML tags, their inferred datatypes, and their configurable semantic roles. This file is a key interface between **raw XML observation** and **intentional semantic enrichment** via roles.

It enables downstream agents (especially `SemanticRdfAgent`) to selectively emit RDF/XML based on human-curated tagging, while retaining full transparency and manual editability.

## Mission

* Analyze XML content during the stream
* Count distinct values per tag
* Infer **XSD primitive datatypes** from lexical analysis
* Emit one `.txt` file per tag in `tags/` to hold values (used in role assignment)
* Generate `summary.tsv` file with fields for semantic staging

## Output Format: `summary.tsv`

| Column             | Description                                                      |
| ------------------ | ---------------------------------------------------------------- |
| `tag`              | Local name of the XML tag                                        |
| `count`            | Number of distinct string values for the tag                     |
| `filename`         | Path to the corresponding tag value file (e.g., `tags/name.txt`) |
| `inferredDatatype` | Lexically inferred XSD primitive datatype (`xsd:date`, etc.)     |
| `tagRole`          | User-defined class/property for the tag (editable)               |
| `stringRole`       | User-defined interpretation of the tag's values (editable)       |

> Only the first three fields are computed by default. Others are meant for manual configuration.

## Inference Strategy

The agent includes a default lexical classifier:

* `true|false` → `xsd:boolean`
* Integer → `xsd:decimal`
* Decimal with exponent → `xsd:double`
* `yyyy` → `xsd:gYear`
* `yyyy-MM` → `xsd:gYearMonth`
* `yyyy-MM-dd` → `xsd:date`
* `yyyy-MM-ddTHH:MM:SS` → `xsd:dateTime`
* Otherwise → `xsd:string`

This inferred datatype is made available to the `SemanticRdfAgent` as sugar for semantic RDF without requiring user configuration.

## Responsibilities

* Traverse XML content (tags and text) in a streaming fashion
* For each tag:

  * Record unique string values
  * Infer datatype
  * Write values to `tags/{tag}.txt`
* Emit `summary.tsv` with all fields
* Ensure directory structure exists

## Collaborators

| Agent              | Role in Collaboration                                        |
| ------------------ | ------------------------------------------------------------ |
| `RoleAgent`        | Guides which tags have semantic roles assigned               |
| `SemanticRdfAgent` | Uses the `summary.tsv` and `.txt` files to emit RDF/XML      |
| `LexiconAgent`     | May be used to validate or expand inferred datatype coverage |
| `XmlToRdf`         | Coordinates and invokes the CSVW emission logic              |

## Design Considerations

* Avoid duplicate values — deduplicate before writing `.txt`
* Prefer sorted output (alphabetical or lexical) for readability
* Fail gracefully if directories cannot be created
* Support reruns without crashing (overwrite allowed)

## Codex Instructions

* When XML is parsed, collect tag-text pairs
* Infer datatype using provided rules
* Store values and emit `summary.tsv` with fields in correct order
* Do not emit roles unless configured externally
* Allow `summary.tsv` to act as a **bridge** to RDF via other agents

## Future Enhancements

* Add provenance to record when/why values were inferred
* Support CSVW metadata generation (`.csvw.json`)
* Incorporate fuzzy or multilingual datatype inference
* Allow spreadsheet-based role configuration and round-trip editing

