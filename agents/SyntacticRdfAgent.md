
# Agent: SyntacticRdfAgent

## Summary

The `SyntacticRdfAgent` is responsible for generating **RDF/XML statements that reflect the syntactic structure** of the original XML document. These RDF statements are always emitted, regardless of whether semantic roles are present. The purpose of this agent is to provide a **lossless, structurally faithful RDF serialization** of the XML content.

This agent serves as the **guaranteed fallback** representation — enabling downstream processes to reconstruct the original XML tree or enrich it later without re-parsing the XML itself.

## Mission

* Emit **OWL-valid RDF/XML** for:

  * XML element start tags
  * Attribute names and values
  * Text node content (untyped literals unless semantically staged)
  * Parent-child relationships via `rdfs:member`
* Ensure **no semantic interpretation** unless delegated to the `SemanticRdfAgent`
* Support full preservation of element order and ancestry

## Design Principles

* **Always-on**: Every element and attribute produces at least one RDF triple
* **Tag-centric**: Every element is mapped to a class named `:<Tag>_Tag`
* **Instance creation**: Every element instance gets an IRI and is typed
* **Attributes become data properties**: Attribute key → property; value → literal
* **Parent-child links**: Each nested element is added via `rdfs:member`

## Example (from `<author name="John">Matthew</author>`)

```xml
<author name="John">Matthew</author>
```

Syntactic RDF/XML emitted (default namespace `ex:` assumed):

```xml
<rdf:Description rdf:about="ex:author_1234">
  <rdf:type rdf:resource="ex:Author_Tag"/>
  <ex:name>John</ex:name>
  <rdfs:member rdf:resource="ex:author_1234_text"/>
</rdf:Description>

<rdf:Description rdf:about="ex:author_1234_text">
  <rdf:type rdf:resource="ex:Text_Tag"/>
  <rdf:value>Matthew</rdf:value>
</rdf:Description>
```

## Responsibilities

* Declare syntactic classes (`:<Tag>_Tag`) for every element
* Create unique IRIs for each element instance (e.g., `ex:tag_hash`)
* Use `rdf:type` for class assertions
* Use `rdfs:member` for nesting
* Create `rdf:value` literals for text
* Declare attribute key-value pairs as RDF property–literal pairs

## Interactions

| Agent              | Interaction Purpose                                                |
| ------------------ | ------------------------------------------------------------------ |
| `XmlToRdf`         | Coordinates tag processing and streaming emission                  |
| `RoleAgent`        | Used to ensure roles do not interfere with syntactic emission      |
| `SemanticRdfAgent` | Operates in parallel; avoids duplicating or overriding this output |
| `RdfXmlSpec`       | Validates that emitted RDF/XML is spec-compliant and legal         |

## Implementation Hints

* Always use the local name of the element to form `:<Tag>_Tag`
* Use a hash (e.g., `MurmurHash3`) to distinguish element instances
* For attributes:

  * Property name = local name
  * Literal value = raw string (untyped)

## Codex Instructions

* You MUST emit these RDF/XML blocks even if no semantic role is defined
* Do NOT skip any XML content — this is a structural representation
* Do NOT add labels, OWL classes, or human semantics — delegate to semantic agent
* Always respect RDF/XML syntax rules (e.g., `rdf:resource`, no blank node collisions)

## Future Enhancements

* Support optional base64 encoding of content (for binary-safe lifting)
* Emit lexical annotations (e.g., inferred datatype hints) as RDF comments
* Optionally include `rdf:parseType="Literal"` when content is mixed or unsafe

