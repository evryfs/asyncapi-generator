# Parser architecture

The parser boundary separates document syntax from the AsyncAPI domain model.
YAML and JSON readers produce the same immutable, typed document tree; domain
parsers then map that tree into the existing `AsyncApiDocument` model. Source
locations survive both stages so structural and semantic failures can point to
the file, line, column, and parser path that introduced a value.

## Responsibilities by stage

| Stage | Owns | Does not own |
| --- | --- | --- |
| Document loading facade | One isolated load context, reader selection, root parsing, external reference loading, semantic validation, and returned warnings | Bundling and generation |
| Reader | File format detection, YAML or JSON syntax, duplicate keys, scalar interpretation, and construction of the neutral document contract | AsyncAPI members or domain rules |
| Document contract | Immutable format-independent nodes, document source identity, input format, and source locations shared between producers and consumers | YAML/Jackson/SnakeYAML implementation details or AsyncAPI domain rules |
| Parser-node adapter | Parser paths and registration of reader-provided locations in the load context | Format-specific syntax |
| Domain parser | Supported AsyncAPI object structure, required members, runtime value types, references, and domain-model construction | General schema validation, bundling, or output generation |
| Schema parser | AsyncAPI Schema Object keywords, boolean schemas, references, Multi Format Schema dispatch, and native schema assets | Validation of all JSON Schema keyword combinations or generator support for every schema format |
| External reference loader | URI/path resolution, document identity, JSON Pointer selection, cycle/deduplication guards, category-directed fragment parsing, and loading referenced files | Ordinary inline object traversal or bundling references into one document |
| Validator | Semantic constraints on the parsed model and reference resolution findings | YAML/JSON syntax and parser value typing |
| Bundler and generators | Downstream transformations and generated output | Deciding how input text is read or structurally parsed |

`AsyncApiDocumentLoader` is the file-based core facade. It creates a fresh
`AsyncApiContext`, reads the root document, builds the parser cursor, parses the
domain model, and validates the result. Validation errors are thrown. The
returned `AsyncApiDocumentLoadResult` contains the document and validation
warnings, including a formatter that retains access to source snippets.

Lower-level types remain available for internal integrations and focused tests,
but callers that want a complete, validated document should prefer the facade.
This keeps context ownership out of callers and prevents state from leaking
between loads.

## Why the reader boundary is a typed document tree

Passing `Map<String, Any?>` from SnakeYAML or Jackson directly into domain
parsers makes behavior depend on library-specific runtime values. It also loses
the distinction between a missing member and a member whose value is explicitly
`null`, and requires source locations to be reconstructed after parsing.

The reader instead produces the neutral `document` package contract.
`InputDocument` has a `DocumentObject` root, and the sealed `DocumentNode`
hierarchy represents objects, arrays, strings, numbers, booleans, and null.
Every node has a `SourceLocation`; object members also retain the key location.
Objects and arrays defensively copy their contents and expose unmodifiable
collections, which makes the reader/parser handoff stable for the lifetime of a
load.

SnakeYAML and Jackson remain internal reader implementations. They already
provide the syntax trees and token locations needed for YAML and JSON, while
`DocumentNode` prevents either library's representation from becoming a parser
or public model contract.

## Paths and source ownership

Reader paths begin at `root` and use member names plus array indexes. When an
`InputDocument` becomes a root `ParserNode`, `ParserNodeFactory` prefixes that
path with a context-owned source identifier and registers every location in the
`SourceRepository`. The repository derives source identity from canonical file
paths, so different files with the same filename remain distinct.

Domain objects are registered in `ModelRepository` with the parser node that
created them. This connects model-level validation findings and references back
to reader-owned locations. Diagnostics use one-based line and column values.

Source locations describe the original documents. Parsing external references
does not merge sources, and bundling is a separate downstream transformation.

## Structural parsing and semantic validation

The parser enforces structure required to construct a trustworthy domain model:
whether a member is present, whether a value is an object or array, and whether
scalars and nested generic elements have their expected runtime types. For
example, a channel's `messages` member must be an object; a list is rejected
instead of being assigned synthetic map keys.

The validator owns semantic rules that require interpretation of an already
constructed model, such as supported AsyncAPI versions, cross-field rules, and
reference resolution findings. The facade intentionally invokes both stages,
but their error categories and implementation packages remain separate.

## Reference loading boundary

Domain parsers create typed `Reference` models and assign a
`ReferenceCategoryKey`. Registering a reference delegates external loading to
`AsyncApiExternalContext`. Internal references stay in the current document and
are resolved from the model registry; they do not cause file I/O.

For external references, the loader resolves the path relative to the document
that contains the reference, canonicalizes file identity, reads the target with
the same reader boundary, and selects the JSON Pointer target. A complete
external AsyncAPI document is parsed and validated as a document. A fragment
file is parsed and validated with the domain parser selected by its reference
category. Loaded documents and fragments are tracked independently to prevent
cycles and repeated work.

This eager loading is currently triggered during reference registration. The
trigger is a context collaboration point, not permission for ordinary domain
parsers to open files themselves. Reference path resolution, target selection,
and external validation must stay in the external-loading package.

## Package boundary rather than module boundary

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

## Stability constraints

For valid supported contracts, parser changes must preserve equivalent YAML and
JSON domain models, the `AsyncApiDocument` API, reference categories and registry
behavior, source ownership, supported external references and schema fragments,
Native and Multi Format Schema behavior, and downstream bundled and generated
output. Malformed-input behavior may become stricter only when the change is
deliberate, source-aware, tested, and documented.
