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

## Configuration

Package `configuration` turns what a user configured into a
`GeneratorConfiguration`.

- `GeneratorSettings` mirrors the documented settings one to one and keeps
  their raw values. The CLI, Maven and Gradle only copy their own syntax into
  it; none of them checks a setting itself.
- `GeneratorConfigurationFactory.create` runs every check and reports each
  problem under the setting's public name, such as `modelPackage` or
  `clientConfig.topicParameterProperties`. It throws on the first invalid
  setting. Two resolvers hold the parts with their own rules:
  `ClientConfigurationResolver` checks client settings on every execution,
  including one without `clientPackage`, because builds share them between
  executions; `SchemaConfigurationResolver` selects the schema artifacts.
- `GeneratorName` selects a `GeneratorProfile`: source code in a
  `SourceLanguage`, schemas of a `SchemaType`, or a document in a
  `DocumentFormat`.
- `ModelType` knows the language that writes it, or none for native Avro and
  Protobuf models, and whether it accepts `modelConfig.modelAnnotation`.
  `SourceLanguage` derives its default and supported model types from it.
- Valid settings that have no effect, such as shared client settings on an
  execution without `clientPackage`, become a `ConfigurationWarning` instead
  of being ignored silently.

## Stages

1. **Plan** (`plan`). `GenerationPlanner` turns the configuration into an
   ordered list of `GenerationTask`s: the bundled document, models, Kafka key
   models, the Spring Kafka client, Avro Projection, native Avro, native
   Protobuf, and JSON Schema. Each task refers to the configuration value it
   plans, such as `ClientGeneration` or a `SchemaGeneration`, so a setting is
   defined once. The planner adds only the order, the source language of the
   client, and the Kafka key-model task, which a client needs when native Avro
   or Protobuf models carry the payloads. A request that only writes documents
   skips the remaining stages.
2. **Prepare input** (`catalog`, `normalizer`, `analyzer`, `messaging`, `input`).
   `GenerationInputFactory` builds the schema catalog, then analyzes channels:
   - `SchemaCatalogLoader` declares every component schema and every message
     payload under its generated name (the last pointer segment in PascalCase),
     plus a model for each object Kafka key. Source models and contract
     declarations claim names separately, and two different schemas under one
     name fail with a name collision. A component schema used only in headers
     gets no source model.
   - `SchemaNormalizer` flattens `allOf` and projects if/then/else into plain
     properties.
   - `SchemaAnalyzer` adds the schemas they reference, infers `type: string`
     for untyped string enums, promotes inline objects and enums to named
     schemas (`InlineSchemaAnalyzer`; different inline types under one name
     fail), and records which unions each schema belongs to.
   - `ChannelAnalyzer` (`messaging`) reads each channel and its messages into
     `AnalyzedChannel`s. A message has a source-code name (its `name`, or its
     key in the channel), a payload (an AsyncAPI schema, a native Avro or
     Protobuf schema, or none), its headers merged from its traits and itself,
     and its `KafkaKey`. The Kafka key is analyzed once, with the name of its
     model when it is an object; the schema catalog declares that model from
     the same analysis.
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
in the language-neutral model (such as `EnumLiteralNormalizer` in `ir`) or in
shared helpers (such as `JsonPropertyAccessAnnotationMapper`) that both
emitters call, so both languages produce the same JSON. When adding a
feature, put the decision in shared code and only the syntax in the emitters.

Spring Kafka clients follow the same split. `SpringKafkaChannelContractFactory`
(`kafka.spring`) builds a language-neutral contract for each channel, whose
payloads, keys, and headers are typed by `TypeRef` with the same scalar
mapping as models; a payload class generated from a native Avro or Protobuf
schema is a `TypeRef.External`. `SpringKafkaClientViewFactory`
(`kafka.spring.client`) turns that contract into the consumer and producer
views, and `SpringKafkaClientEmitter` renders them with the templates of the
language. The few differences between Kotlin and Java, such as type names,
nullability, and documentation markup, come from a `SpringKafkaClientSyntax`:
`KotlinClientSyntax` or `JavaClientSyntax`.

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
