# Bundler Strategy

The bundler transforms a parsed AsyncAPI model into a self-contained model that
can be passed to generation or document output without access to the source
documents that supplied its references.

## Boundary contract

`AsyncApiBundler` accepts an `AsyncApiDocument` that has already been parsed,
validated, and resolved by the loader. Reference objects retain their resolved
models, source identity, and reference fragments. The bundler does not perform
file I/O, parse input, load or resolve references, validate semantic rules, or
generate artifacts.

The public entry point is `AsyncApiBundler.bundle(AsyncApiDocument)`. The
bundler traverses the supported AsyncAPI object model and updates existing
reference state so that resolved models are available where generation and
document output need them.

## Reference traversal

Traversal identity is the pair of `Reference.sourceId` and `Reference.ref`.
The fragment alone is not sufficient: the same fragment can identify different
models in different source documents. Traversal contexts are copied as the
bundler enters a reference, so cycle detection does not mutate the parent
traversal path while promotion and recursion state remain shared for the root
document.

For ordinary object references, the bundler traverses the model already stored
in `Reference.model` and marks the reference for inline serialization. This
makes the referenced object available in the bundled model without requiring
the generator or document writer to load another file.

Schema-valued protocol binding fields follow the schema traversal policy. For
example, an external schema used by `bindings.kafka.key` is serialized into the
binding so the bundled document remains independent of the source schema file.

## Schema references and cycles

Schema references have a narrower policy because schemas can be recursive:

- Non-recursive external schemas may be inlined into the referencing model.
- Recursive external schemas are promoted into the root
  `components.schemas` collection.
- Recursive edges are rewritten as local component references to the promoted
  schema.
- A promoted name collision with a root schema or another promoted schema
  fails explicitly.

This policy supports self-recursive and mutually recursive external schemas
while ensuring that the bundled document has local definitions for the cycle.
Boolean schemas and multi-format schemas remain their native model forms; the
bundler does not cast multi-format schemas to the AsyncAPI `Schema` type.

## Output guarantee and boundaries

For supported parsed models, bundling produces a model whose traversed
references and promoted recursive schemas are self-contained for generation and
portable document output. Self-contained does not mean that the bundler
performs a complete AsyncAPI or JSON Schema transformation, normalizes every
possible reference category, or formats YAML and JSON. Serialization details
remain the responsibility of the document-output boundary.

Keeping this work at the model boundary simplifies generation and allows a
bundled document to be written or transferred without the original external
source files.

See [bundled document examples](examples.md) for complete, verified input and
output documents covering the supported reference behaviors.
