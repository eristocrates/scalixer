# AGENTS.md

> Snapshot as of 2025-06-30  
> Focus: Semantic Class Staging, Unary Roles, and Promotion Strategy  
> Purpose: Document logic and guidance for semantic configuration

---

## 🧠 AGENT: SemanticClassPromotionAgent

### Summary
Promotes syntactic XML tags and strings into semantic OWL classes and named individuals, based on a default-first configuration model. All promotions can be suppressed or redirected via role configuration files.

### Default Behavior
- **Tags** become OWL Classes (semantic promotion)
- **Strings** inside tags become Named Individuals of that class
- **Datatypes** override promotion if inferred type is not `xsd:string`
- **Suppression** is achieved via empty substitution in config

### Core Heuristics
- Tag `Name` → OWL Class `:Name`
- String `"Microformats"` inside `Name` → Named Individual `:Name_Microformats`
- Literal `2014` → `xsd:gYear`, not promoted
- URI pattern match (e.g. `http://`, `mailto:`) → `xsd:anyURI`

### Configuration Paths
- `roles/TagRoles.txt` → Tag-level class mapping (or suppression)
- `roles/StringRoles/<tag>.txt` → Per-tag datatype overrides

---

## 🧱 AGENT: SyntacticSemanticBoundaryAgent

### Purpose
Maintains boundary between syntactic representations (`:Name_Tag`) and semantic constructs (`:Author`, `:Microformats`, etc.)

### Policy
- Do **not** use `owl:equivalentClass` between syntactic and semantic
- Instead, trace using:
  - `prov:wasDerivedFrom`
  - `rdfs:label`
- Separate syntactic RDF/XML always emitted
- Semantic RDF/XML optionally emitted (based on config)

---

## 🔄 AGENT: UnaryRoleStagingAgent

### Summary
Applies unary roles to tags and strings. Used to:
- Promote/suppress tags to OWL classes
- Promote/suppress strings to OWL individuals
- Override datatypes

### Unary Role Matrix
| Axis X     | Axis Y     | Default Role         | Overridable? |
|------------|------------|----------------------|--------------|
| Tag        | Unary      | Promote to Class     | ✅            |
| String     | Unary      | Promote to Individual| ✅            |
| String     | Unary      | Infer xsd datatype   | ✅            |

---

## 🧱 AGENT: StringlessTagHandler

### Summary
Handles tags that have no literal content, only nested tags or attributes.

### Behavior
- Included in `summary.tsv` with no string content
- Eligible for class promotion, especially at the root
- Treated as structural containers
- May later be linked to:
  - `skos:Concept`
  - `owl:Class`
  - `prov:Entity` (if metadata-bearing)

---

## 👪 AGENT: ParentChildBinaryRoleAgent

### Summary
Defines binary roles between child tag/string and their **syntactic parent**.

### Policy
- Only relationships to **parent** are considered (not arbitrary siblings)
- Attributes are children of their tag
- Configurable in future for:
  - `hasX` property assignment
  - inverse property toggle
  - suppression (blank value)

---

## 📚 AGENT: DefaultHeuristicAgent

### Purpose
Hard-codes common intuitive mappings to aid user experience without explicit staging.

### Examples
- Tag `Year` → literal interpreted as `xsd:gYear`
- Tag `Version` → `xsd:string` but could promote if configured
- URI-like strings → `xsd:anyURI`
- Empty tags → `owl:Nothing` or class-only placeholder

---

## 🔧 AGENT: RoleConfigSystemAgent

### Summary
Declares the structure and intent of the `roles/` folder.

### Files
- `roles/TagRoles.txt` — tag name → semantic class
- `roles/StringRoles/<tag>.txt` — string literal → datatype
- *(planned)* `roles/BinaryTagToParent.txt` — tag name → semantic property
- *(planned)* `roles/BinaryStringToParent.txt` — string literal → semantic property

### Behavior
- A blank value suppresses promotion
- A non-blank value renames or retypes

---

## 🧩 AGENT: IdentityProfileAgent

### Purpose
Lays groundwork for identity vs profile modeling:
- `:Author_Microformats` as identity
- Enrich with SKOS-XL or FOAF later
- Profiles handled via separate extension layer

---

## 🔍 AGENT: DiagnosticSummaryAgent

### Summary
Emits summary TSV files for staging review:
- `summary.tsv` — all tags and their strings
- `summary-stringless.tsv` — tags with no string value
- `summary.csvw` — inferred datatypes

---

## 🧠 Future Tasks

- Implement binary role config system
- Add `skos:prefLabel` and fallback `rdfs:label` everywhere
- Structure inverse relation toggles
- Enrich identity with SKOS, FOAF, and PROV
- Support nested parent-child relation chains for deeper semantics

---

> This `AGENTS.md` is a living document. Regenerate or update it as the system grows.
