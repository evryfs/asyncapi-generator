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
the loaded document. All state of one run lives in a `BundlingContext` created
for that run.

## Rules

Each object bundler (`channels`, `messages`, `operations`, `schemas`, and so
on) walks its part of the model and replaces resolved references:

- A reference to a non-schema object in another file is replaced by the
  bundled object.
- A reference to a component schema of the root document stays a reference
  (`#/components/schemas/<Name>`). The component itself is bundled once with
  the root components.
- A reference that leaves the root document and reaches a component schema,
  or a recursive schema, is promoted: the schema is added to the bundled
  `components.schemas` under the last segment of its pointer and the reference
  is rewritten to point there. `PromotedSchemaRegistry` tracks promoted
  schemas by model identity and by name. A second schema with the same name
  and the same serialized content reuses the first; different content fails
  with `AsyncApiBundlingException.PromotedSchemaNameCollision`.
- Other schema references are inlined. `BundlingContext` tracks visited
  references so recursion through inlined schemas terminates;
  `SchemaRecursionAnalyzer` identifies recursive schemas that must be promoted
  instead.

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
