# Let’s build a **taxonomy of XML tag roles** and **string content roles**, designed for the purpose of RDF/OWL semantic lifting.

These roles will become the *discrete values* you can later use in heuristics, schema-based discriminators, or user configuration like `assumeTagEntities = true`.

## 🏷️ **Tag Roles** (XML Tags)

These describe the *semantic function* of an XML tag — that is, *what kind of RDF structure the tag implies*. A tag can be:

### 1. `EntityTag`

> Tag defines a **real-world entity** or **domain instance**, often represented as an RDF `owl:Class` or instance of a class.

**Examples:**

```xml
<Book>...</Book>
<Author>...</Author>
```

**RDF:**

```ttl
ex:Book a owl:Class .
ex:book1 a ex:Book .
```

---

### 2. `PropertyTag`

> Tag expresses a **relationship** or **attribute** of its parent. Typically becomes an RDF property.

**Examples:**

```xml
<Title>Semantic Web for Dummies</Title>
<Price>29.99</Price>
```

**RDF:**

```ttl
ex:book1 ex:title "Semantic Web for Dummies" .
```

---

### 3. `ContainerTag`

> Tag is purely **structural**, grouping child elements. It has no semantic value by itself.

**Examples:**

```xml
<Items>
  <Item>...</Item>
</Items>
```

**RDF:**

> Might emit `rdf:List`, `rdfs:member`, or `ex:hasItem`.

---

### 4. `TypenameTag`

> Tag name is a **type label**, and the tag implies typing of its content.

**Examples:**

```xml
<String>hello</String>
<Integer>42</Integer>
```

**RDF:**

```ttl
ex:hasValue "hello"^^xsd:string .
```

---

### 5. `WrapperTag`

> Tag wraps **metadata** or **non-content information** about another entity.

**Examples:**

```xml
<Metadata>
  <CreatedBy>...</CreatedBy>
</Metadata>
```

---

### 6. `ReferenceTag`

> Tag represents a **pointer** to another entity, often via ID or URI.

**Examples:**

```xml
<SeeAlso>urn:isbn:12345</SeeAlso>
```

**RDF:**

```ttl
ex:thisDoc rdfs:seeAlso <urn:isbn:12345> .
```

---

### 7. `AnnotationTag`

> Tag conveys **labels, descriptions, or comments**, but does not assert factual data.

**Examples:**

```xml
<Label>This is a note</Label>
<Comment>Check this later</Comment>
```

**RDF:**

```ttl
ex:thing rdfs:label "This is a note" .
```

---

## 📜 **String Roles** (XmlString/Text Content)

These define the role of *text nodes*, often nested inside tags. Strings can be:

### 1. `LabelString`

> Text is a **human-readable label** for a resource.

**Examples:**

```xml
<Author>J.K. Rowling</Author> <!-- label -->
```

**RDF:**

```ttl
ex:jk rdfs:label "J.K. Rowling" .
```

---

### 2. `LiteralValueString`

> Text is a **literal value**, to be interpreted as a `xsd:string`, `xsd:decimal`, etc.

**Examples:**

```xml
<Price>12.99</Price>
```

**RDF:**

```ttl
ex:item ex:price "12.99"^^xsd:decimal .
```

---

### 3. `IdentifierString`

> Text is a **named identifier** or code for a resource, not meant for display.

**Examples:**

```xml
<ISBN>9781234567890</ISBN>
```

**RDF:**

```ttl
ex:book ex:isbn "9781234567890"^^xsd:string .
```

---

### 4. `ReferenceString`

> Text is a **link or URI** that points elsewhere.

**Examples:**

```xml
<Homepage>https://example.org</Homepage>
```

**RDF:**

```ttl
ex:author ex:homepage <https://example.org> .
```

---

### 5. `ClassValueString`

> Text expresses a **concept or category**, not a concrete value.

**Examples:**

```xml
<Genre>Science Fiction</Genre>
```

**RDF:**

```ttl
ex:book ex:hasGenre ex:ScienceFiction .
```

---

### 6. `EmptyString`

> Text is **whitespace or blank**, and should usually be ignored.

---

### 7. `MixedContentString`

> Text is part of **mixed content**, often within narrative.

**Examples:**

```xml
<Para>This book is <b>bold</b> in its claims.</Para>
```

**RDF:**
May need to use `rdf:XMLLiteral`.

---

## ✅ Summary (Discriminator Value Sets)

```scala
enum TagRole:
  case EntityTag, PropertyTag, ContainerTag, TypenameTag,
       WrapperTag, ReferenceTag, AnnotationTag

enum StringRole:
  case LabelString, LiteralValueString, IdentifierString,
       ReferenceString, ClassValueString, EmptyString, MixedContentString
```

These enums can form the foundation for:

* `assumeTagRole: Tag => TagRole`
* `assumeStringRole: (Tag, Text) => StringRole`
