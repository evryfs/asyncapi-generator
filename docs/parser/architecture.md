# Parser architecture

The parser boundary separates document syntax from the AsyncAPI domain model.
YAML and JSON readers produce the same immutable, typed document tree; domain
parsers then map that tree into the existing `AsyncApiDocument` model. Source
locations survive both stages so structural and semantic failures can point to
the file, line, column, and parser path that introduced a value.

## Responsibilities by stage

| Stage                     | Owns                                                                                                                                                                        | Does not own                                                                                    |
|---------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| Document loading facade   | One isolated load context, reader selection, root parsing, external reference loading, semantic validation, returned warnings, and canonical source dependencies                    | Bundling and generation                                                                         |
| Reader                    | File format detection, bounded file access, YAML or JSON syntax and safety limits, duplicate keys, scalar interpretation, and construction of the neutral document contract | AsyncAPI members or domain rules                                                                |
| Document contract         | Immutable format-independent nodes, document source identity, input format, and source locations shared between producers and consumers                                     | YAML/Jackson/SnakeYAML implementation details or AsyncAPI domain rules                          |
| Parser-node adapter       | Parser paths and registration of reader-provided locations in the load context                                                                                              | Format-specific syntax                                                                          |
| Domain parser             | AsyncAPI version-profile selection, supported object structure, required members, runtime value types, references, and domain-model construction                            | General schema validation, bundling, or output generation                                       |
| Schema parser             | AsyncAPI Schema Object keywords, boolean schemas, references, Multi Format Schema dispatch, and native schema assets                                                        | Validation of all JSON Schema keyword combinations or generator support for every schema format |
| External reference loader | URI/path resolution, document identity, JSON Pointer selection, cycle/deduplication guards, category-directed fragment parsing, and loading referenced files                | Ordinary inline object traversal or bundling references into one document                       |
| Validator                 | Semantic constraints on the parsed model and reference resolution findings                                                                                                  | YAML/JSON syntax and parser value typing                                                        |
| Bundler and generators    | Downstream transformations and generated output                                                                                                                             | Deciding how input text is read or structurally parsed                                          |

`AsyncApiDocumentLoader` is the supported file-based core facade. It creates a
fresh `AsyncApiContext`, reads the root document, builds the parser cursor,
parses the domain model, and validates the result. Validation errors are thrown.
The returned `AsyncApiDocumentLoadResult` contains the document, deduplicated root
and external validation warnings, a source-aware warning formatter, and the
canonical files consumed by the load. The source set includes the root,
external documents, and native Avro or Protobuf assets so build frontends can
track complete incremental inputs.

Lower-level document nodes, readers, parser cursors, individual parsers,
contexts, repositories, registries, resolvers, and individual validators are
internal implementation APIs. Core tests can exercise them through Kotlin's
test friend-path support, but external consumers cannot assemble a partial
pipeline that omits loading or validation. The domain model, loader facade,
load result, findings, source locations, and caller-relevant exceptions remain
public. This keeps context ownership out of callers and prevents state from
leaking between repeated or concurrent loads.

## Why the reader boundary is a typed document tree

Passing `Map<String, Any?>` from SnakeYAML or Jackson directly into domain
parsers makes behavior depend on library-specific runtime values. It also loses
the distinction between a missing member and a member whose value is explicitly
`null`, and requires source locations to be reconstructed after parsing.

The reader instead produces the neutral `document` package contract.
`InputDocument` has a `DocumentNode` root, and the sealed node hierarchy
represents objects, arrays, strings, numbers, booleans, and null. This lets the
reader represent every syntactically valid YAML or JSON root without applying
the AsyncAPI object-root rule; the domain parser owns that structural
requirement. Every node has a `SourceLocation`; object members also retain the
key location. Objects and arrays defensively copy their contents and expose
unmodifiable collections, which makes the reader/parser handoff stable for the
lifetime of a load.

SnakeYAML and Jackson remain internal reader implementations. They already
provide the syntax trees and token locations needed for YAML and JSON, while
`DocumentNode` prevents either library's representation from becoming a parser
or public model contract.

## Paths and source ownership

Reader paths begin at `root` and use member names plus array indexes. When an
`InputDocument` becomes a root `ParserNode`, `ParserNodeFactory` creates a
`NodeAddress` from a context-owned source identifier and typed member/index
segments. Repositories use that address as identity; they never recover
identity by parsing the rendered diagnostic path. Canonical file ownership
keeps same-named files in different directories distinct.

Simple names retain familiar paths such as
`contract.root.components.schemas.Order`. Names containing dots, brackets,
quotes, backslashes, or control characters use quoted bracket notation, such as
`contract.root.components.schemas["Order.properties.id"]`. An object member
named `"0"` is a member segment, while `[0]` is an array-index segment. Display
paths remain presentation data and cannot collide in repository storage.

Domain objects are registered in `ModelRepository` with the parser node that
created them. This connects model-level validation findings and references back
to reader-owned locations. Diagnostics use one-based line and column values.

Source locations describe the original documents. Parsing external references
does not merge sources, and bundling is a separate downstream transformation.

## Structural parsing and semantic validation

The parser enforces structure required to construct a trustworthy domain model:
whether a member is present, whether a value is an object or array, whether
scalars and nested generic elements have their expected runtime types, and
whether an ordinary fixed-field object contains only fields selected by its
version profile. For example, both `Operation.action` and `Operation.channel`
are structurally required, and a channel's `messages` member must be an object.
Unknown fields on ordinary objects fail at the member key; permitted `x-`
extensions remain accepted. Patterned maps, bindings, Schema Objects, and
deliberately free-form values keep their specialized policies.

Reference-versus-inline discrimination happens before ordinary fixed-member
checking. A present `$ref` must be a string. Once it is valid, it selects a
Reference Object and its siblings are ignored as required by AsyncAPI 3.0. A
null or non-string `$ref` fails at `$ref` instead of falling through to inline
parsing.

The parser selects an implemented AsyncAPI major/minor profile before mapping
the root object. Version selection must happen before structural parsing because
the selected specification defines which object members and shapes can be
interpreted. Patch versions share their major/minor profile. The validator owns
semantic rules that require interpretation of an already constructed model,
such as cross-field rules and reference resolution findings. The facade
intentionally invokes both stages, but their error categories and implementation
packages remain separate.

Parser profiles travel with parser nodes rather than the shared load context.
This permits complete external AsyncAPI documents to select their own profile
while raw external fragments inherit the profile of the document that refers to
them. Version-specific behavior belongs behind the profile; domain parsers must
not compare raw version strings.

## Reference loading boundary

Domain parsers create typed `Reference` models and assign a
`ReferenceCategoryKey`. Registering a reference delegates external loading to
`AsyncApiExternalContext`. Internal references stay in the current document and
are resolved from the model registry; they do not cause file I/O.

For external references, the loader resolves a relative path or `file:` URI
against the document that contains the reference, canonicalizes file identity,
reads the target with the same reader boundary, and selects the decoded JSON
Pointer target. A complete external AsyncAPI document selects its own parser
profile and is parsed and validated as a document. A raw fragment inherits the
originating profile and is parsed and validated with the domain parser selected
by its reference category. Loaded documents and targets are tracked
independently to prevent cycles and repeated work.

This eager loading is currently triggered during reference registration. The
trigger is a context collaboration point, not permission for ordinary domain
parsers to open files themselves. Reference path resolution, target selection,
and external validation must stay in the external-loading package.

## Resource safety and local-file trust

Reader limits bound each source document before parsing. A separate budget is
owned by one complete loader call and accounts for the root, external documents,
unique reference targets, external-reference depth, aggregate bytes, and native
schema assets. Canonical files and previously processed targets count once, so
cycles do not repeatedly consume the budget. A limit failure becomes a
structured parser diagnostic at the reference that attempted the load.

Native schema assets use the same strict UTF-8 policy as documents and are read
with a bounded operation rather than `readText()`. They count toward the
aggregate source budget and are returned in the load result's source set.

Local relative paths and `file:` URIs are supported. HTTP and other remote
schemes are not. The loader does not impose a project-root sandbox: local files
referenced by a contract are trusted build input. Any future sandbox or remote
loading feature requires an explicit configuration and security design rather
than an implicit parser change.

## Package and supported-API boundaries

The document, reader, parser, context, loader, validator, bundler, and generator
packages currently live in the core Maven module because they share the domain
model and are used together by core consumers. The reader and parser depend on
the neutral document contract rather than on each other. Their dependency
direction is clear enough to enforce with package APIs and tests. Splitting
them into Maven modules would add publication and dependency-management costs
without removing the necessary model and context collaboration.

A module split would become useful only if the reader or parser needed an
independent release lifecycle, a meaningfully smaller dependency surface, or a
standalone supported artifact. Until then, package boundaries are the simpler
long-term design.

Within the core module, Kotlin `internal` visibility distinguishes pipeline
implementation from supported API. This is intentional even for useful types
such as `DocumentNode`: the reader-to-parser contract is an architectural
boundary inside core, not a promise that consumers can replace one stage while
depending on its internals. A new public type should be introduced only for a
documented consumer capability that cannot be expressed through the loader,
domain model, findings, locations, or exceptions. Making an implementation
type public merely to simplify tests is not sufficient.

## Stability constraints

For valid supported contracts, parser changes must preserve equivalent YAML and
JSON domain models, the `AsyncApiDocument` API, reference categories and registry
behavior, source ownership, supported external references and schema fragments,
Native and Multi Format Schema behavior, and downstream bundled and generated
output. Malformed-input behavior may become stricter only when the change is
deliberate, source-aware, tested, and documented.
