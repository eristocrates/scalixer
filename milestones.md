# Action plan
Below is a **concrete, staged action plan** that builds from your current syntactic lifting baseline toward a system that can **casually round-trip semantically enriched data** between heterogeneous application-specific XML tag models (what we’ll call *XML profiles*), such as **ESRI** and **Infor**. I’ll follow with the **very next actionable step**, per your suggestion.

## 🧭 High-Level Maturity Target

> Enable lifting of XML from Application A (e.g., ESRI Workspace), semantic enrichment via RDF, and *casual* lowering into Application B’s XML profile (e.g., Infor Stormwater), **without requiring schema validation or formal ontology mapping yet**.

## ✅ Assumptions

* Each application has an *XML profile* (set of tags, tag roles, and known structural idioms).
* Users configure *semantic alignment* via:

  * `owl:sameAs`, `rdfs:subClassOf`, `rdfs:label`, etc.
  * Role `.txt` files and potentially tag-class alignment files.
* RDF is always emitted in **RDF/XML syntax**.
* **Syntactic RDF** is mandatory and complete.
* **Semantic RDF** is optional and end-user guided.
* Lowering is driven by **target profile selection**, not schema validation.

---

## 🧱 Core Milestones

### 1. **Syntactic RDF is Exhaustively Emitted** (✅ Nearly Done)

**Goal**: Every XML feature (tag, string, attribute, nesting) must emit valid RDF/XML, classed as:

* `:xmlTag`, `:xmlAttribute`, `:xmlString`
* Connected via `rdfs:member`, `:attribute`, `:xmlString` (as `rdf:Literal`)

➡ **Next Action**:
Update `sbt run rdf` to *always emit full syntactic RDF/XML* even when string role `.txt` files are empty (with assumed primitive typing from lexicon inference).

---

### 2. **XML Profiles Are Formalized (Lift-Time Attachment)**

**Goal**: Identify and tag source application XML profile during lifting.

* Option 1: Use user config (e.g. `sourceProfile := "esri"`).
* Option 2: Infer from file paths, root element, or known tags.
* Tag each `:xmlTag` individual with:

  ```ttl
  :featureClass_tag_1 :sourceProfile :esri .
  ```

➡ **Next Action**:
Introduce a **profile annotation hook** in `XmlToRdf.scala` (via a `sourceProfile` variable) that tags all lifted tags with a profile URI or literal.

---

### 3. **Semantic Alignment via Profiles**

**Goal**: Enable user-defined correspondence across profiles (e.g., ESRI `:FeatureClass` and Infor `:StormAsset`)

* End user adds:

  ```ttl
  :FeatureClass owl:sameAs :StormAsset .
  ```
* These are stored in a `semantic-alignments.ttl` file, imported as static RDF during lifting/lowering.

➡ **Next Action**:
Allow optional inclusion of `alignments/semantic-alignments.ttl` during lifting and lowering to enrich or override mappings.

---

### 4. **Semantic Enrichment Occurs Between Lift & Lower**

**Goal**: After lifting from Profile A, user or agent adds domain knowledge:

* Class memberships (e.g., `a :MajorOutfall`)
* Attributes like `:hasLocation`, `:hasDiameter`
* Labeling or linking (e.g., `rdfs:label`, `skos:definition`)
* Optional ontology-based augmentation (outside scope for now)

➡ **Next Action**:
Nothing new; this is enabled already via RDF editing or external inference tools. Make sure syntactic output remains intact regardless of semantic addition.

---

### 5. **Lowering Respects a Target Profile**

**Goal**: Lower enriched RDF to **another profile’s XML** by:

* Matching individuals with known `owl:sameAs`, `rdfs:subClassOf`, or tagged with a new `:targetProfile` directive.
* Rendering tags using the correct name for the target profile.

➡ **Next Action**:
Build a prototype `sbt run xml` lowering tool that:

* Accepts `--targetProfile infor`
* Rewrites tag names from `:xmlTag` individuals via lookup into a profile-specific vocabulary map (backed by `owl:sameAs` or user-defined YAML/TSV)

---

### 6. **Round-Trip Provenance and Fidelity Auditing**

**Goal**: Maintain traceability from input XML → RDF → output XML:

* Reversible unique naming of tags
* Round-trip logging and validation
* Attach provenance metadata (`prov:wasDerivedFrom`, `:sourceXmlPath`, etc.)

➡ **Next Action**:
Stage `ProvenanceAgent` to annotate lifted entities with source file, tag location, and tag name mapping.

---

## 🛠 Summary of Next Steps

| Step | Task                                   | Agent/Module          |
| ---- | -------------------------------------- | --------------------- |
| ✅ 1  | Emit RDF even with empty string roles  | `XmlToRdf.scala`      |
| 🔜 2 | Attach source XML profile as triple    | `XmlToRdf.scala`      |
| 🔜 3 | Load `semantic-alignments.ttl`         | `SemanticRdfAgent`    |
| 🔜 4 | Build `sbt run xml` lowering prototype | `LoweringAgent` (TBD) |
| 🔜 5 | Accept `--targetProfile` CLI arg       | Lowering agent        |
| 🔜 6 | Map RDF individuals to output tags     | Profile vocab lookup  |
