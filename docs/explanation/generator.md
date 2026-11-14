# Generator strategy

This page is for maintainers changing code generation in
`asyncapi-generator-core`, package
`dev.banking.asyncapi.generator.core.generator`. The user-level view is
[How the generation pipeline works](architecture.md).

## Input and entry point

`AsyncApiGenerator.generate(bundled, configuration)` accepts only a
`BundledAsyncApiDocument`, which only the bundler can create, and a
`GeneratorConfiguration` built by `GeneratorConfigurationFactory`. Unbundled
or unvalidated documents therefore cannot reach generation. The generator
receives a `ProtocProvider` from its frontend; it never decides on its own how
`protoc` is obtained.

## Stages

1. **Plan** (`plan`). `GenerationPlanner` turns the configuration into a list
   of `GenerationTask`s: models, Kafka key models, Spring Kafka clients,
   Avro Projection, native Avro, native Protobuf, JSON Schema, and the bundled
   document. A request that only writes documents skips the remaining stages.
2. **Prepare input** (`input`, `analyzer`, `normalizer`). `GenerationInputFactory`
   collects the schemas and channels of the document, normalizes schemas, and
   analyzes them: inline objects and enums are promoted to named schemas
   (`InlineSchemaAnalyzer`), and polymorphic relationships are recorded. Name
   collisions between different inline types fail here.
3. **Check compatibility** (`input`). `GenerationInputCompatibilityValidator`
   checks the prepared input against the planned tasks, for example that
   native payloads have an output that handles them and that native model
   packages match the schemas. These are target-specific failures; see
   [Validation boundary](../contributing/validation-boundary.md).
4. **Render** (`artifact` and the format packages). Each task renders
   `GeneratedArtifact`s in memory. Nothing is written until every task has
   rendered.
5. **Write** (`output`). `FileSystemGeneratedArtifactWriter` rejects two
   artifacts with the same destination, stages every file, skips files whose
   content is unchanged, and replaces the rest atomically where the file
   system allows. With a `GeneratedOutputManifest` it then removes files the
   execution no longer generates.

## Language-neutral models

Kotlin and Java models are built from one language-neutral model in `ir`.
`ModelDefinitionFactory` turns the analyzed schemas into `ModelDefinition`s:
`ObjectModel`, `EnumModel`, `UnionModel`, and `OpenObjectModel`, with
properties typed by `TypeRef` and defaults as typed `DefaultValue`s.
`ModelTypeMapper` decides the `TypeRef` of each schema once, for both
languages.

Emitters render that model:

- `KotlinModelEmitter` (`kotlin`) renders data classes, enums, sealed
  interfaces, and type aliases;
- `JavaModelEmitter` (`java`) renders classes or records, enums, and
  interfaces.

Decisions that affect the wire format, such as enum constant names and their
`@JsonProperty` values, property identifiers for invalid names, access
annotations for `readOnly` and `writeOnly`, and discriminator names, are made
in the language-neutral model or in shared helpers in `model` (such as
`EnumLiteralNormalizer` and `JsonPropertyAccessAnnotationMapper`) that both
emitters call, so both languages produce the same JSON. When adding a
feature, put the decision in shared code and only the syntax in the emitters.

Spring Kafka clients follow the same split: `SpringKafkaChannelContractFactory`
builds a language-neutral channel contract, and the Kotlin and Java client
emitters render it.

## Schema formats

- `avro`: Avro Projection from AsyncAPI Schema Objects
  (`AvroSchemaGenerator`, `AvroTypeMapper`) and native Avro schemas and
  SpecificRecords (`NativeAvroGenerator`). Invalid Avro names fail with
  `InvalidAvroName`; they are never rewritten.
- `protobuf`: native Protobuf schemas and `protoc` compilation
  (`NativeProtobufGenerator`), with `ProtocProvider` implementations for each
  frontend.
- `jsonschema`: Draft 7 files (`JsonSchemaGenerator`).

## Tests

Generated output is protected by ApprovalTests under
`asyncapi-generator-core/src/test/resources/approvals/generator`, and by the
system-test scenarios, which compile and run the generated code. Update an
approval only when output changes deliberately. See
[Development setup](../contributing/development-setup.md).
