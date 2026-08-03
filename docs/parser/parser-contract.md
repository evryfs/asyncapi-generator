# Parser contract

This reference defines the current file-reading and parser-facing contract in
`asyncapi-generator-core`.

## Public file-loading entry point

```kotlin
val result = AsyncApiDocumentLoader().load(file)
val document = result.document
val warnings = result.warnings
val renderedWarnings = result.formatWarnings()
val sourceFiles = result.sourceFiles
```

Each call creates an isolated `AsyncApiContext`. The loader accepts `.yaml`,
`.yml`, and `.json` files, reads and structurally parses the root document,
eagerly loads supported external references, and runs semantic validation.
Reader, parser, external-reference, and validation errors are thrown. A
successful result contains the existing `AsyncApiDocument` domain model and an
immutable list of validation warnings. Warnings found while validating external
documents or selected fragments are returned with root warnings, deduplicated
by rule and source location, and are never logged by parser infrastructure.
`formatWarnings()` returns an empty string when no warnings exist and otherwise
renders source-aware snippets.

`sourceFiles` is an immutable, deterministic set of canonical `File` values. It
contains the root document, every loaded external document, and every external
native Avro or Protobuf schema asset. Canonical files appear once even when
multiple references or cycles reach them. Build frontends use this set for
incremental input tracking.

The loader is stateless. Every call owns a new context, repositories, warning
collector, resource budget, and reader instances. Repeated and concurrent calls
therefore cannot share parser state.

Bundling and generation are not performed by this API.

The loader, load result, domain model, validation findings, source locations,
and caller-relevant diagnostics and exceptions form the supported API. The
document tree, concrete readers, cursor types, contexts, repositories,
registries, resolvers, version implementations, individual parsers, and
individual validators are internal to the core module.

## Reader contract

Inside the loader, `DocumentReaderRegistry` selects a reader by case-insensitive
file extension. The registry, its `DocumentSource` overload, and both reader
implementations are internal; external callers use `AsyncApiDocumentLoader`.

Both readers return an `InputDocument` whose root is a `DocumentNode` and whose
contents use the same semantic node categories:

These shared types live in the neutral `document` package. Reader
implementations produce them, while the parser and other source-aware consumers
depend on them without depending on a concrete YAML or JSON reader.

| Node              | Plain runtime value                                    | Location                                           |
|-------------------|--------------------------------------------------------|----------------------------------------------------|
| `DocumentObject`  | `Map<String, Any?>`                                    | Object start; every member also has a key location |
| `DocumentArray`   | `List<Any?>`                                           | Array start; every element has its own location    |
| `DocumentString`  | `String`                                               | Scalar token                                       |
| `DocumentNumber`  | `Int`, `Long`, `BigInteger`, `Double`, or `BigDecimal` | Scalar token                                       |
| `DocumentBoolean` | `Boolean`                                              | Scalar token                                       |
| `DocumentNull`    | `null`                                                 | Null token                                         |

Readers reject empty documents, malformed syntax, invalid mapping keys, and
duplicate keys with `DocumentReadException`. A syntactically valid document may
have an object, array, scalar, or null root; readers do not apply the AsyncAPI
requirement that the root be an object.

Document objects always have string member names. JSON enforces this in its
grammar; YAML mapping keys must resolve as strings and cannot be collections,
numbers, booleans, or null. Quoted YAML keys such as `"true"` and `"42"` remain
valid strings. This prevents YAML-only key coercion from producing a document
tree that could not be represented by equivalent JSON. YAML merge keys are
rejected rather than being exposed as literal `<<` members; shared members must
be written explicitly.

YAML custom tags and unsupported node kinds are malformed input.
Only JSON-compatible numeric spellings are accepted: YAML-only octal forms,
non-finite numbers, and other YAML numeric representations are rejected rather
than silently becoming different values. Timestamp-looking values remain
strings. Plain `yes`, `no`, `on`, and `off` remain strings, while actual
`true` and `false` tokens become booleans. A UTF-8 BOM is accepted, CRLF input
retains correct source locations, and additional YAML or JSON documents after
the root are rejected.

The AsyncAPI parser requires the root cursor to contain an object. Any other
root produces an `unexpected-value-type` parser diagnostic at the root source
location. This keeps format syntax errors separate from AsyncAPI structural
errors.

## AsyncAPI specification versions

Before parsing the domain structure, `AsyncApiParser` requires `asyncapi` to be
a string in `major.minor.patch` form with an optional alphanumeric suffix. The
declared value is preserved in `AsyncApiDocument`, while its major/minor line
selects an `AsyncApiParserProfile`. Patch releases share the same profile.

The parser recognizes the published 3.0 and 3.1 specification lines but only
the 3.0 parser profile is currently implemented. Therefore `3.0.x` documents
are supported, including suffixed values such as `3.0.0-rc1`; `3.1.x` produces
a diagnostic explaining that the version is known but its parser profile has
not been implemented. Other version lines are unsupported rather than being
silently interpreted as 3.0.

Profiles are carried by parser nodes. Complete external AsyncAPI documents
select their own profile, while raw external fragments inherit the profile of
the reference that loads them. Version-dependent parsing must use this profile
instead of comparing the raw `asyncapi` string.

File access failures are normalized as `UnreadableDocument`; Jackson and
SnakeYAML exceptions do not escape as the top-level failure. Both readers apply
the same default limits of 20 MiB for UTF-8 input and decoded document length,
plus a nesting depth of 100. YAML additionally permits at most 50 expanded
collection aliases. Numeric tokens are limited to 1,000 characters in both
formats. Size, depth, alias, and numeric-token violations are reported as
`ResourceLimitExceeded`. An unrecognized SnakeYAML exception is
`MalformedDocument`, not a resource-limit failure.

`DocumentReadException` exposes structured information independently of its
message: every subtype has `file`, failures with a known source mark have
`location`, `DuplicateKey` has `memberName`, `UnsupportedFormat` has `format`,
and `ResourceLimitExceeded` has `limit` and `maximum`. The original cause is
retained where a library, decoding, or file-access failure exists.

File input is decoded as strict UTF-8. Malformed byte sequences are rejected as
`MalformedDocument` rather than being silently replaced before syntax parsing.

YAML presentation details such as quoting and block style do not survive as
semantic data. Quoted numbers and booleans remain strings. JSON-compatible YAML
booleans `true` and `false` become booleans; YAML 1.1 words such as `yes`, `no`,
`on`, and `off` remain strings.

YAML and JSON use the same numeric representation. Integers use `Int` or `Long`
when in range and `BigInteger` otherwise. Decimals remain `Double` when their
lexical value survives the conversion exactly; values that would lose decimal
precision or overflow use `BigDecimal`. This preserves ordinary runtime values
while preventing format-dependent truncation.

## Parser cursor API

`ParserNode` is a source-aware cursor over one `DocumentNode`. Structural
navigation is available only after selecting an object or array view.

| Owner              | Operation                     | Contract                                                                                |
|--------------------|-------------------------------|-----------------------------------------------------------------------------------------|
| `ParserNode`       | `expectObject()`              | Requires an object and returns its object navigation view                               |
| `ParserNode`       | `expectArray()`               | Requires an array and returns its indexed navigation view                               |
| `ParserNode`       | `expect<T>()`                 | Checks the complete requested Kotlin runtime type, including nested list and map values |
| `ParserNode`       | `toPlainValue()`              | Recursively removes source metadata and returns maps, lists, scalars, or null           |
| `ParserObjectNode` | `required(name)`              | Requires the named member to be present and returns its cursor                          |
| `ParserObjectNode` | `optional(name)`              | Returns the member cursor or `null` when absent                                         |
| `ParserObjectNode` | `members()`                   | Returns one cursor per member, preserving names and paths                               |
| `ParserObjectNode` | `membersStartingWith(prefix)` | Returns matching member cursors with their original names and paths                     |
| `ParserObjectNode` | `expectOnlyMembers(...)`      | Rejects members outside an object's supported set and allowed specification extensions  |
| `ParserArrayNode`  | `elements()`                  | Returns one cursor per element with indexed paths                                       |

`expect<T>()` performs type checking, not coercion. A string is not converted to
a boolean or number, and a scalar is not wrapped in a collection. Map keys and
nested collection elements are checked recursively, so
`expect<Map<String, List<Boolean>>>()` cannot hide an invalid nested value behind
an unchecked cast.

`expect<Any?>()` and `toPlainValue()` both allow any JSON-compatible value.
`expect<T>()` should be preferred when the AsyncAPI field has a defined shape;
`toPlainValue()` communicates that source metadata is intentionally discarded
at a free-form boundary.

## Fixed-field object policy

The AsyncAPI 3.0 parser profile owns the allowed-member set for each ordinary
fixed-field object. Domain parsers select the relevant object type and do not
repeat large field sets locally. An unknown non-extension member produces
`parser.unexpected-object-member` at the member key. A specification extension
is accepted when its name matches `x-` followed by letters, digits, dots,
underscores, or hyphens.

This policy applies to ordinary AsyncAPI objects such as Info, Server, Channel,
Message, Operation, Components, Security Scheme, and their traits and nested
fixed-field objects. It does not reinterpret patterned maps, bindings, Schema
Objects, or deliberately free-form values as fixed-field objects.

Unconditional required fields are parser-owned. In particular, an inline
Operation Object requires both `action` and `channel`. Whether the referenced
channel resolves to the correct semantic target remains validator-owned.

## Absent and null values

Absence and explicit null are different parser states.

| Input state        | Object view `optional("field")` | Object view `required("field")`  | `expect<String>()`               | `expect<String?>()` |
|--------------------|---------------------------------|------------------------------------|----------------------------------|---------------------|
| Member absent      | `null`                          | Missing-required-member diagnostic | Not applicable                   | Not applicable      |
| Member is `null`   | Cursor over `DocumentNull`      | Cursor over `DocumentNull`         | Unexpected-value-type diagnostic | `null`              |
| Member is a string | Cursor over `DocumentString`    | Cursor over `DocumentString`       | String value                     | String value        |

Callers must not use a nullable expectation merely to make malformed nulls
disappear. Use it only when the corresponding domain contract permits explicit
null. When presence itself is significant, retain the member cursor separately;
`SchemaParser` does this for `default` through its `defaultSet` flag.

`Channel.address` is a specification-defined nullable exception: absence and
explicit null both produce a null domain value, while a non-string, non-null
value is a structural parser error. JSON Schema nullability is expressed with a
type union such as `type: [string, "null"]`; a Schema Object `type: null` is not
a nullable schema.

## Parser diagnostics

Strict parser failures throw
`AsyncApiParseException.ParserDiagnosticFailure`. Its `diagnostic` is structured
data; the exception message is a human-readable rendering with a source snippet.

Every `ParserDiagnostic` exposes:

- a stable category and string code;
- expected type or condition;
- actual value category and actual plain value when applicable;
- parser path; and
- `SourceLocation`, containing source identifier, file, one-based line, and
  one-based column.

Current categories are:

| Code                                       | Meaning                                                                  |
|--------------------------------------------|--------------------------------------------------------------------------|
| `parser.missing-required-member`           | An object does not contain a required member                             |
| `parser.unexpected-object-member`          | An object contains a member that its parser does not recognize           |
| `parser.unexpected-value-type`             | A value or nested value has the wrong runtime type                       |
| `parser.invalid-specification-version`     | The `asyncapi` value does not have the required version form             |
| `parser.unsupported-specification-version` | The declared version has no implemented parser profile                   |
| `parser.invalid-reference`                 | A reference is not a supported URI reference with JSON Pointer semantics |
| `parser.reference-document-not-found`      | The external document does not exist or is unreadable                    |
| `parser.reference-target-not-found`        | The JSON Pointer target does not exist in the loaded document            |
| `parser.load-resource-limit-exceeded`      | One complete load exceeded a configured source/reference resource budget |

Diagnostic messages may include targeted hints for common quoted scalar
mistakes. The category and structured fields, rather than rendered prose, are
the stable surface for programmatic assertions.

## Internal identity and display paths

Repositories identify nodes with a canonical source identifier and typed
member/index segments. A rendered dot path is never parsed to recover identity.
This distinguishes a component named `A.properties.x` from property `x` below
component `A`, and distinguishes an object member named `"0"` from array index
`0`.

Simple names keep the existing display form. Ambiguous names use quoted bracket
notation with JSON string escaping. For example:

```text
contract.root.components.schemas.Order
contract.root.components.schemas["Order.properties.id"]
contract.root.values["0"]
contract.root.values[0]
```

`SourceLocation.path` and parser diagnostic paths are presentation values. The
source and model repositories use the segment-aware address directly.

## Reference behavior

A Reference Object must be an object containing a string `$ref`. A missing
member produces a missing-required-member diagnostic where a Reference Object
is required; an explicit null or a non-string value produces an
unexpected-value-type diagnostic at `$ref`. For reference-versus-inline fields,
a present `$ref` controls discrimination. Once it is a valid string, sibling
members are ignored as required by AsyncAPI 3.0 and are not checked against the
inline object's member policy. Domain parsers attach the concrete
`ReferenceCategoryKey` used to parse external fragments; the category is not
inferred from substrings in the reference path.

Internal references do not load a file. Local relative paths and `file:` URIs
are resolved relative to the source document that owns the reference. HTTP and
other remote schemes are unsupported. Canonical file paths distinguish
same-named files in different directories. URI percent encoding is decoded for
file paths, and JSON Pointer `~0` and `~1` escapes are supported when selecting
targets. Numeric-looking object keys remain object members rather than being
mistaken for array indexes.

External targets have two modes:

- A target document with an `asyncapi` member is parsed and validated as a full
  AsyncAPI document.
- A fragment-only document is parsed and validated using the reference category,
  such as schema, message, channel, operation, server, parameter, or binding.

An external fragment container does not need to be an AsyncAPI document or have
an object root. JSON Pointer selection can traverse object or array containers,
and a root scalar can be selected directly when the reference category permits
that value, such as a boolean Schema Object. The selected target must satisfy
the structure required by its reference category. Only the selected target is
parsed; unrelated siblings in a heterogeneous raw file are not interpreted as
members of the same category. A whole-file Message reference therefore selects
one Message Object at the document root. A raw map containing multiple named
messages is a container rather than one Message Object and must select an
individual message with an explicit JSON Pointer; the parser does not bulk
import or splice container members.

References discovered inside a selected raw fragment retain their concrete
category and resolve same-file JSON Pointers on demand. Reference chains and
cycles are deduplicated by file, pointer, category, and parser profile. A
missing same-file target is reported at the nested reference in the raw
fragment that owns it.

Loading is eager and deduplicated. It preserves each file's source locations and
does not bundle or inline the model.

One loader call applies these additional load-wide defaults:

| Resource | Default maximum |
| --- | ---: |
| Distinct canonical source documents | 256 |
| Unique resolved file-and-pointer targets | 4,096 |
| Acyclic external-reference depth | 64 |
| Aggregate canonical source bytes | 64 MiB |
| One native Avro or Protobuf schema asset | 20 MiB |

Canonical sources and previously processed targets count once. Cycles therefore
terminate without repeatedly consuming the budget. Native assets count toward
aggregate bytes and use bounded strict UTF-8 decoding. A limit failure is
`parser.load-resource-limit-exceeded` and exposes the limit, configured maximum,
observed value, and source location of the reference that caused the load.

Referenced local files are trusted build input. The loader deliberately has no
project-root sandbox; adding one requires a separate configuration design.

## Schema Object behavior

`SchemaParser` is specialized because Schema Objects are recursive and permit
shapes that ordinary AsyncAPI objects do not:

- Boolean schemas are represented as `SchemaInterface.BooleanSchema` only when
  the YAML or JSON value is an actual boolean. Quoted `"true"` and `"false"`
  values are strings and fail structural parsing.
- String `$ref` values produce schema references.
- An absent `type` is valid. A present `type` is parsed as a string, an array of
  strings, or an explicit null retained through field registration. The
  validator reports explicit null with the stable `JSONSCHEMA-TYPE` finding;
  it is not treated as absence. Nullable schemas use a union such as
  `type: [string, "null"]`.
- Recursive keywords such as `properties`, `items`, `allOf`, `anyOf`, `oneOf`,
  `not`, conditional schemas, and schema-valued dependencies recurse through
  `SchemaParser`.
- The current domain model represents `items` as one schema. Tuple validation
  expressed as an array of schemas is not represented in `Schema.items`; the
  parser retains the original field value and location in the model repository
  so `SchemaValidator` can report the unsupported form with a source-aware
  diagnostic.
- Property dependencies are lists of strings; schema dependencies are parsed as
  schemas.
- `default`, `const`, `examples`, and enum values preserve JSON-compatible plain
  values. `defaultSet` distinguishes an absent default from an explicit null.
- Known AsyncAPI Schema Object formats delegate back to ordinary schema parsing.
  Known native or other Multi Format Schema values are preserved as
  `MultiFormatSchema`; native Avro and Protobuf schemas may load supported
  external schema assets.
- Unknown `schemaFormat` values are rejected by the existing schema-format
  contract.

Schema Objects are not subject to ordinary AsyncAPI fixed-member rejection.
Unknown and unsupported keywords remain registered with their original values
and locations so `SchemaValidator` can issue dialect or capability findings.
Referencing a fragment from an OpenAPI-like or otherwise heterogeneous file
does not switch parser dialects: the selected fragment still follows AsyncAPI
3.0 Schema Object and JSON Schema Draft 7 behavior, while unrelated surrounding
content is ignored.

The schema parser constructs model shape. `SchemaValidator` remains responsible
for semantic keyword policy and combinations.

## Extensions and free-form values

Extension members beginning with `x-` and specification fields explicitly
defined as free-form cross the typed parser boundary as JSON-compatible plain
values: `Map<String, Any?>`, `List<Any?>`, `String`, `Number`, `Boolean`, or
`null`.

Use `toPlainValue()` only at those boundaries. The returned value has no
`SourceLocation`, so the containing model and its registered field locations
remain the source of downstream diagnostics. Known structural fields must use
`expect<T>()`, `expectObject()`, or `expectArray()` instead of being treated as
free-form.
