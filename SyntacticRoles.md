# 🏷️ `TagRole` Documentation

| TagRole           | Description                                                                                                         | Example XML                                                          | RDF Emission Strategy                                                                                                |
| ----------------- | ------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| **EntityTag**     | Represents a discrete RDF individual (an entity), often with child tags expressing its properties.                  | `<Book>`                                                             | Emits a new named individual with an `rdf:type` derived from the tag name.                                           |
| **PropertyTag**   | Denotes a property of the current subject. Its object may be a literal (string) or another entity (nested element). | `<title>Semantic Web</title>` or `<Author><Name>Tim</Name></Author>` | Emits a predicate triple. If contents are string: `DatatypeProperty`. If contents are nested tags: `ObjectProperty`. |
| **ContainerTag**  | An XML wrapper with no direct RDF meaning; exists solely to group children.                                         | `<Metadata><Title>...</Title></Metadata>`                            | Ignored in RDF typing; children are attached directly to the grandparent, optionally via `rdfs:member`.              |
| **TypenameTag**   | Holds a string that names the *class* of the entity it belongs to. Often used to emit an `rdf:type`.                | `<type>Book</type>`                                                  | Emits an `rdf:type` triple using the string content as the object (resolved to a class).                             |
| **ReferenceTag**  | Denotes a reference to another RDF entity, typically via identifier or pointer.                                     | `<authorId>123</authorId>`                                           | Emits an `owl:sameAs`, `ex:hasAuthor`, or similar link to another entity, using the string content as a URI or ID.   |
| **AnnotationTag** | Provides additional metadata or human-readable information about a subject, such as comments or descriptions.       | `<comment>This is deprecated.</comment>`                             | Emits `rdfs:comment`, `skos:note`, or custom annotation properties.                                                  |
| **CollectionTag** | Groups multiple instances of the same type, such as lists or arrays.                                                | `<Authors><Author>...</Author><Author>...</Author></Authors>`        | Emits `rdfs:member` triples for each child. May also support RDF containers like `rdf:Bag`.                          |

---

## 🧵 `StringRole` Documentation

| StringRole             | Description                                                                             | Example Text Content         | RDF Emission Strategy                                                                             |
| ---------------------- | --------------------------------------------------------------------------------------- | ---------------------------- | ------------------------------------------------------------------------------------------------- |
| **LabelString**        | Serves as a human-readable label or name.                                               | `"Semantic Web for Dummies"` | Emits `rdfs:label`, `skos:prefLabel`, or similar.                                                 |
| **LiteralValueString** | Encodes a primitive value, such as number, date, or boolean.                            | `"2023-05-01"` or `"42"`     | Emits as an `xsd:...`-typed literal (e.g., `xsd:date`, `xsd:decimal`).                            |
| **IdentifierString**   | A unique identifier for the entity. May be used for IRI construction or de-duplication. | `"B1234"`                    | Used to construct entity IRI (e.g., `:Book_B1234`), or populate `dc:identifier`.                  |
| **ReferenceString**    | A pointer to another known entity.                                                      | `"Author123"`                | Emits a triple with a URI derived from the string.                                                |
| **ClassValueString**   | Indicates the *type* of the subject in string form.                                     | `"Book"` or `"UrbanFeature"` | Used in `rdf:type` statements (interpreted as OWL Class).                                         |
| **EmptyString**        | Indicates that the tag was present but had no content.                                  | `""` (empty)                 | May be ignored, or used to trigger special fallback logic.                                        |
| **MixedContentString** | A string that is interleaved with child elements (mixed XML content).                   | `"Hello <em>world</em>!"`    | Either discarded, extracted as plain text, or processed into `rdf:value` plus nested annotations. |

---

### ✅ Notes for Implementation

* A tag can be matched to a **TagRole** via presence in its corresponding `roles/TagRole/*.txt` file.
* A tag's **StringRole** is inferred at runtime via heuristics or preconfiguration (from `roles/StringRole/*.txt`).
* Every tag’s string content is written to `tags/<tag>.txt`, and the summary TSV+CSVW contains inferred datatype and configurable roles.
* The presence of `CollectionTag` or `ContainerTag` triggers recursive RDF expansion, especially useful for generating `rdfs:member`.
