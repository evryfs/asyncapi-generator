# Schema validation library evaluation

## Decision

The validator does not currently add `networknt/json-schema-validator` for Schema Object conformance. Schema rules continue to operate on the parsed, source-located domain model using Kotlin and Java numeric types.

This is a boundary decision, not a judgment that the library is unsuitable for JSON Schema generally. It can validate schemas against bundled Draft 7 meta-schemas and supports custom dialects, vocabularies, keywords, and formats. Those capabilities are documented by the [NetworkNT project](https://github.com/networknt/json-schema-validator).

## Fit with this validator

The validator receives an `AsyncApiDocument`; it must not reopen or reinterpret source text. NetworkNT validates a Jackson tree. Supplying one here would require serializing or rebuilding each parsed Schema Object, then mapping its JSON Pointer results back to the original model instances and `SourceLocation` values. That reconstructed tree would not retain the original external file, parser path, line, and column by itself.

AsyncAPI Schema Objects also extend Draft 7 with fields such as `discriminator`, `deprecated`, `externalDocs`, and `bindings`. Using the Draft 7 meta-schema directly would therefore remain incomplete, while a custom dialect would introduce a second keyword registry alongside the generator-capability policy already required by this project.

The library's default regular-expression implementation uses Java regular expressions rather than ECMA-262. Its own documentation recommends optional Joni or GraalJS integrations for closer compatibility. Adopting it would therefore not remove the separate pattern-engine decision.

The generator emits Schema Object patterns through Jakarta Validation's `@Pattern`, whose contract uses `java.util.regex.Pattern`. The validator consequently checks that runtime as a generator-capability boundary and preserves the exact value when escaping generated Java and Kotlin source. AsyncAPI says a pattern SHOULD use ECMA-262 rather than making that dialect a conformance requirement, so adding a JavaScript runtime solely for schema-authoring diagnostics would impose a disproportionate dependency and execution surface.

## Consequence

The current implementation uses the source-located domain graph for traversal and diagnostics, `BigInteger` and `BigDecimal` for exact numeric decisions, and explicit presence flags where Kotlin `null` must remain distinguishable from an absent keyword. It implements only rules that can be mapped to an AsyncAPI or Draft 7 requirement, or to a documented generator capability. Generator-only inference, such as deriving `string` for an untyped all-string enum, occurs after validation in generation analysis and does not mutate parser output.

This decision should be revisited if a library can consume the existing domain representation without losing source ownership, or if a later instance-validation use case replaces enough custom behavior to justify a shared engine.
