# Validator

## What it does

The validator applies read-only rules to the linked model that the [parser](parser.md) produced, and reports findings. A finding has a rule, a message, a source location, and a documentation link. Errors stop the load; warnings are returned with the document.

## Contract

- **Input:** The linked `AsyncApiDocument`, or a document or fragment loaded for a reference, and the per-load `AsyncApiContext`.
- **Output:** A `ValidationReport` with the findings in document order.
- **Invariants:**
  - The validator never changes the model. Reference links come from the parser; the validator only reports their problems.
  - Each object is checked once per run, under the name of the first path that reaches it.
  - Every finding uses the documentation link of its rule.

## Architecture

**`ValidationWalk`** runs one validation. It owns one rule set per kind of object and decides how a member is checked: an inline object by the rules of its kind, a `$ref` by reporting its link and queueing the object it leads to. After the root document has been walked, the walk checks the queued targets that no path reached, naming them `<path> target`.

**Rule sets** (`ChannelRules`, `MessageRules`, `SchemaRules`, …) implement `ObjectRules<T>`. Each one checks its object and walks its children through the walk, in document order, so findings keep the order of the contract. A rule set never resolves a reference or builds another rule set.

**`ValidationPath`** names the object a finding is about, such as `Channel 'orders' Message 'created'` or `Operation 'send' Trait[0]`. Messages start with the path.

**`ReferenceResolver`** turns a reference's link into findings: unresolved, a cycle, a reference of another category, or a target of the wrong kind.

**Document rules** (`DocumentRule`) check how objects across the whole document relate: root operations must use root channels, and Kafka messages need a schema registry when a Kafka server has none. They run after the walk.

**Protocol binding rules** (`ProtocolBindingRules`) check the bindings of one protocol. `BindingRules` looks them up by protocol; Kafka is the first.

**`ValidationRule`** is the identity of a rule: its stable code, concern, severity, and documentation link. `AAS3-` rules link to their AsyncAPI specification section, `JSONSCHEMA-` rules to their JSON Schema keyword, and `GEN-` and `ADV-` rules to their row in the [rule inventory](../reference/validation-rules.md).

**Loaded targets.** `LoadedTargetValidator` validates each external document and fragment on its own before the root document. A fragment is checked as the kind of object its reference expects and named `External <Kind> '<name>'`.

**Rendering.** `core.diagnostics` turns findings into the messages frontends print, each with a snippet of its source.

## What it does not do

The validator does not read files, link references, bundle, or decide what a generator can produce for one output; generation compatibility is checked by the generator (see the [validation boundary](../contributing/validation-boundary.md)).
