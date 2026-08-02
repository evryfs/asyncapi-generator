# Validator architecture

## Responsibility

Validation starts after reading and parsing. It consumes an `AsyncApiDocument` and checks semantic rules that cannot be guaranteed by the parsed Kotlin model alone. It does not reopen source files, reinterpret YAML or JSON syntax, normalize values, resolve documents by reading them, bundle models, or generate output.

The reader owns syntax and source coordinates. The parser owns structural interpretation and domain construction. External loading owns locating and parsing referenced documents or fragments. The validator owns semantic conformance, generator capability limits, advisories, and reference integrity over the resulting model graph.

## Data flow

```text
AsyncApiDocument
    -> AsyncApiValidator
    -> AsyncApiValidationProfile
    -> domain validators and reference resolution
    -> ValidationCollector (invocation-local mutable state)
    -> ValidationReport (immutable public result)
    -> ValidationReporter or frontend-specific rendering
```

`AsyncApiValidator` selects the validation profile once from the parsed specification version. Validators receive the collector selected for that invocation; they do not compare raw version strings.

`ValidationCollector` is mutable only while one validation is running. Each finding is created from a catalogued `ValidationRule`. `ValidationReport` snapshots the findings and exposes read-only `findings`, `errors`, and `warnings` lists. Logging and exception construction happen after validation through `ValidationReporter`, except where a frontend intentionally controls its own output channel.

The CLI, Maven plugin, and Gradle plugin enter this pipeline through `AsyncApiDocumentLoader`. The loader creates one request-local context and owns reading, parsing, external loading, and semantic validation before returning the document and immutable warnings. Frontends retain responsibility for their own warning output, bundling, generation configuration, and generated-output registration.

The collector also owns invocation-local identity sets and a queue of resolved reference targets. Domain validators record each reference edge with its concrete expected category. Resolution follows reference-to-reference chains, checks the final target category, and queues reachable targets. `AsyncApiValidator` drains that queue through explicit category dispatch. A model instance is entered once, so shared targets do not duplicate findings and reference cycles terminate without recursion.

Root operation validation receives the root channel registry explicitly. Component operations do not receive that boundary because the specification allows them to reference channels in other locations. Operation and reply message subsets are checked against the direct entries owned by the selected channel before following any nested reference chain. This distinction accepts `operation -> channel message -> component message` while rejecting an operation that bypasses its channel and points directly to the component message.

Semantic format checks operate on the exact strings stored in the domain model. Validators do not trim, unquote, or otherwise normalize values. URI syntax uses `java.net.URI`, media types use Jakarta Activation, email addresses use Jakarta Mail, and runtime-expression fragments use Jackson's JSON Pointer parser.

Schema Object validation likewise operates on the parsed, source-located model. Numeric constraints use `BigInteger` and `BigDecimal` semantics rather than narrowing through `Double` or `Int`. The [schema validation library evaluation](schema_validation_library_evaluation.md) explains why Draft 7 meta-validation is not currently delegated to a separate Jackson-tree validator.

An enum without `type` remains untyped in the parsed domain model. Generation analysis may derive `string` only for an all-string enum because the Java and Kotlin generators require a concrete model type. Untyped enums containing other values produce a generator-capability finding instead of being silently coerced. Contradictory lower and upper constraints are also reported as generator-capability errors: JSON Schema permits an unsatisfiable schema, but emitted validation annotations cannot represent that intent safely for generated contracts.

Recursive Schema Object traversal covers properties, pattern properties, definitions, dependency schemas, array applicators, composition, conditionals, and tuple members. Boolean schemas terminate traversal normally. Draft 7 tuple-form `items` remains present in the parsed model so its members receive source-aware validation, but generation reports a capability error because Java and Kotlin collection types cannot retain position-specific tuple constraints. The established single-schema `items` API remains unchanged.

Generator analysis of required object fields uses a declaration scope rather than only the nearest `properties` map. The scope includes enclosing object declarations and `allOf` members reached through internal or external references, with identity-based cycle termination. This prevents composition and conditional schemas from producing misleading undeclared-property warnings while retaining the safeguard for fields the generators genuinely cannot find. AsyncAPI discriminator rules remain deliberately local: the named property must be defined by that Schema Object itself and included in its own `required` list.

Message examples are instance-checked directly against the same parsed Schema Object graph. The checker follows internal and external schema references, terminates recursive schema cycles by schema identity and instance path, preserves exact numeric semantics, and reports the source location of the failing nested example value. It implements the represented Draft 7 validation keywords without rebuilding schemas as a second Jackson tree. Multi Format Schema examples are not interpreted by the ordinary checker; they produce an explicit generator-capability warning until a proven validator exists for that format.

String patterns remain exact parsed values. AsyncAPI recommends ECMA-262 syntax, but generated Jakarta Validation annotations execute patterns through `java.util.regex`. The validator therefore reports Java-incompatible patterns as generator-capability errors instead of misclassifying them as JSON Schema conformance errors. Generation escapes the exact pattern for its Java or Kotlin source literal without trimming or removing characters. A JavaScript runtime is not added solely to enforce the specification's non-mandatory regex recommendation.

Schema keyword policy is explicit. Keywords represented by the parsed Draft 7 and AsyncAPI Schema Object model are supported; `x-` fields are preserved extensions; known OpenAPI or newer-dialect keywords produce targeted capability findings; ignored annotations produce warnings; and unknown fields produce capability errors rather than disappearing silently. The Schema Object `$schema` keyword may select the supported Draft 7 dialect. Multi Format `schemaFormat` selection is a parser responsibility: known AsyncAPI wrappers become ordinary Schema Objects, while known Draft 7, Avro, OpenAPI, RAML, and Protobuf formats remain typed `MultiFormatSchema` values and are not reinterpreted by the ordinary validator.

Protocol binding content remains free-form in the public domain model and in serialized output. The parser additionally records validation metadata for each protocol binding: protocol name, owning server/channel/operation/message location, exact binding version value, and parsed schema-valued fields. Binding validators consume that metadata instead of guessing whether a map is wrapped or which vocabulary applies. Kafka 0.4.0 and 0.5.0 are supported explicitly. Unknown protocols are preserved without speculative generic warnings, while known Kafka fields are checked against their location-specific vocabulary and their embedded Schema Objects continue through ordinary schema validation.

Kafka constraints that span objects run after ordinary reference traversal. For each root channel, the validator resolves the channel's selected servers, or all root servers when `servers` is absent or empty, and relates its effective message bindings to those servers. Schema-ID fields are permitted only when every applicable Kafka server declares `schemaRegistryUrl`. Unused component messages remain reusable definitions and are not assigned a server context until a root channel uses them.

## Finding contract

Every finding contains:

- a stable `code` suitable for tests and tooling;
- a `concern`: `SPECIFICATION`, `GENERATOR_CAPABILITY`, or `ADVISORY`;
- a `severity`;
- human-readable `message` text;
- one `SourceLocation`, when the parsed value owns a location;
- an authoritative or explanatory documentation link.

The parser path, file, line, and column belong to `SourceLocation`. `ValidationFinding.path` and `ValidationFinding.line` are derived conveniences; they are not independently mutable coordinates.

Messages remain useful to people but are not stable machine identifiers. Tests should normally assert the rule code, severity, and source location rather than freeze complete prose.

## Rule ownership

`SPECIFICATION` findings enforce AsyncAPI 3.0, the supported JSON Schema dialect, or an official protocol binding. `GENERATOR_CAPABILITY` findings describe otherwise valid input that this generator cannot safely consume. `ADVISORY` findings are optional guidance and must not be presented as specification conformance.

The [rule inventory](rules.md) records the implemented rule catalog, its authorities, source ownership, and focused test owners. Stable codes are the machine-facing contract; adding a code requires a defensible rule classification and matching tests and documentation.

## External fragments

An external AsyncAPI document is parsed and validated as a document. A fragment selected from a non-AsyncAPI document is parsed and validated only as the requested AsyncAPI object category. Unrelated surrounding OpenAPI, JSON Schema, or plain YAML content is outside the validator boundary.

External fragment validation uses the AsyncAPI 3.0 profile because that is the only parser and validator profile implemented. Reachable fragment targets enter the same category-specific traversal as inline objects, while unrelated content in the surrounding foreign document remains outside the validation boundary.
