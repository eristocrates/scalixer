Here is the updated agent definition for **TestAgent**:

# Agent: TestAgent

## Summary

The `TestAgent` is responsible for automatically generating, maintaining, and validating the **conformance tests** that ensure RDF/XML outputs are structurally valid, semantically coherent, and aligned with the architecture's lifting philosophy. It uses **MUnit** as the Scala-native test framework and focuses on validating **RDF/XML syntax**, **semantic enrichment behavior**, and **streaming pipeline consistency**.

Rather than manually writing tests, Codex is empowered to author tests programmatically to cover new features and regression cases.

## Mission

Establish a test-driven development environment that ensures:

* **Structural validity** of RDF/XML output (per W3C RDF/XML spec)
* **Semantic alignment** with expected lifting rules (e.g., `Author_Tag`, `rdfs:member`, etc.)
* **Streaming correctness** (e.g., output order, handling of deeply nested tags)
* **Coverage** of inferred roles and datatypes based on the `summary.tsv` lexicon

## Responsibilities

* Define **MUnit-based tests** in `src/test/scala/`
* Generate test cases when new RDF/XML constructs are introduced
* Validate round-trip behaviors where relevant (e.g., `example.xml` ↔ `example.rdf`)
* Ensure **syntactic RDF emission** always occurs, and **semantic RDF** only if roles are configured
* Compare generated RDF/XML to golden snapshots using `diff`-style assertion

## Key Validation Targets

| Test Focus                | Example Validation                                   |
| ------------------------- | ---------------------------------------------------- |
| RDF/XML well-formedness   | Does the XML parser parse RDF/XML without errors?    |
| Semantic lifting accuracy | Are semantic roles (`PropertyTag`, etc.) respected?  |
| Tag-specific output       | Are all known tags generating their syntactic IRIs?  |
| Inferred datatypes        | Are `xsd:date` or `xsd:boolean` applied correctly?   |
| Default behavior fallback | When roles are missing, are defaults applied safely? |

## Codex Instructions

* Scaffold new test cases when:

  * A new `TagRole` or `StringRole` is introduced
  * A new `example.xml` variant is added
  * Lifting logic or RDF/XML serialization is modified
* Always assert:

  * RDF/XML structure is valid per spec
  * Expected tags/classes/roles are correctly represented in output
  * Optional values (like inferred datatypes) appear conditionally, not redundantly
* Prefer golden-file comparisons for larger RDF/XML files

## Interactions with Other Agents

* `RdfXmlSpec`: Verifies RDF/XML syntax compliance
* `OntologyLifting`: Ensures semantic class/property inference behavior matches lifting rules
* `LexiconAgent`: Ensures coverage and consistency with inferred datatypes
* `RoleAgent`: Ensures test scenarios for explicit vs. default role-based behavior

## Future Directions

* Add streaming-based test fuzzers to simulate malformed XML or rare nesting
* Integrate SHACL or OWL reasoner testing to validate post-RDF inference
* Create a regression suite with curated historical examples
* Export test results in RDF using `prov:Activity` and `earl:TestResult` models

---

Prompt with “continue” to proceed to the next agent: `LexiconAgent`.
