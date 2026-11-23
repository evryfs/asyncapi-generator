# Bundler strategy

This page is for maintainers changing the bundler in `asyncapi-generator-core`,
package `dev.banking.asyncapi.generator.core.bundler`. The user-level
behavior is described in
[Bundle multi-file documents](../how-to/bundle-multi-file-documents.md).

## Contract

`AsyncApiBundler.bundle(loaded)` takes the result of
`AsyncApiDocumentLoader.load`, which has already read every referenced file,
resolved every `$ref`, and validated the contract. It returns a
`BundledAsyncApiDocument`, the only input the generator accepts.

The bundler is a pure transformation of the domain model. It does not read
files, parse YAML or JSON, validate, or generate code, and it does not modify
the loaded document. What one bundling shares (promoted schemas, recursion
analysis) lives in a `BundlingRun` created for that call; where the bundler is
in the document (references followed, external-document scope) lives in an
immutable `BundlingScope`.

## Rules

Each object bundler (`channels`, `messages`, `operations`, `schemas`, and so
on) walks its part of the model and replaces resolved references:

- A reference to a non-schema object in another file is replaced by the
  bundled object.
- A reference to a non-schema object follows reference chains to the end: a
  local reference to a component that is itself an external `$ref` is replaced
  by the fully resolved object, so no external `$ref` remains.
- References that point into the root document by position stay references and
  are not inlined: an operation's `channel` and `messages`, and an operation
  reply's `channel` and `messages`. Only a reply's `address` is bundled. The
  targets are root channels, which are bundled themselves.
- Binding schema fields (such as the Kafka message `key`) are always bundled as
  schemas, so a local `$ref` inside an external file is promoted or inlined
  instead of dangling. The bundled model's `kafkaKeySchema` and
  `protocolBindings` hold the bundled schemas. A message key that is an
  external reference is promoted to `components.schemas` under the last segment
  of its pointer (`key-schemas.yaml#/myAccountKey` becomes
  `#/components/schemas/myAccountKey`), so the generated key type keeps the
  name it has when generated from the original files.
- `items` in array form (tuple items) are bundled and take part in recursion
  analysis like every other child schema.
- A reference to a component schema of the root document stays a reference
  (`#/components/schemas/<Name>`). The component itself is bundled once with
  the root components.
- A reference that leaves the root document and reaches a component schema,
  or a recursive schema, is promoted: the schema is added to the bundled
  `components.schemas` under the last segment of its pointer and the reference
  is rewritten to point there. Only a pointer of exactly three segments
  (`#/components/schemas/<Name>`) names a component schema; a deeper pointer
  such as `#/components/schemas/Order/properties/total` is an ordinary schema
  and is inlined. `PromotedSchemaRegistry` tracks promoted
  schemas by model identity and by name. A second schema with the same name
  and the same serialized content reuses the first; different content fails
  with `AsyncApiBundlingException.PromotedSchemaNameCollision`.
- Other schema references are inlined. `BundlingScope` tracks visited
  references so recursion through inlined schemas terminates;
  `SchemaRecursionAnalyzer` identifies recursive schemas that must be promoted
  instead.

Specification extensions (`x-*`) are part of the model of every extensible
object and are written to the bundle.

References are identified by the unique id of the file they were written in, so
same-named files in different directories never share visited or recursion
state.

Keeping and promoting component schema names is what makes code generated
from a bundle identical to code generated from the original files: generated
type names come from schema names. The system tests assert this for every
scenario that writes an `outputFile`.

## Tests

`BundledDocumentApprovalTest` protects complete bundled documents for the
multi-file, fragment, recursive, and component-catalog fixtures under
`asyncapi-generator-core/src/test/resources/approvals/bundler`. The
`bundling` and `external-references` system-test scenarios cover the same
behavior through every frontend.
