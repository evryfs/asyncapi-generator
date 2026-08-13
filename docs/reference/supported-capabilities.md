# Supported capabilities

This reference describes the production workflows currently supported by
`asyncapi-generator`. Support means that the workflow has a typed implementation
and focused verification in this repository. It does not imply complete support
for every AsyncAPI or JSON Schema feature.

## Capability summary

| Capability                   | Supported input                                                                         | Result                                                                                          | Important limitations                                                                                                 | Verified example                                                                                                                                                                                                               |
|------------------------------|-----------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Document loading             | `.yaml`, `.yml`, and `.json` AsyncAPI 3.0.x documents                                   | Source-aware parsed and validated model                                                         | UTF-8 input; resource limits apply; AsyncAPI 3.1 is recognized but not implemented                                    | [Parser contract](../parser/parser-contract.md)                                                                                                                                                                                |
| External references          | Internal references, relative local files, and `file:` URIs with JSON Pointer fragments | Eagerly resolved models with source identity and locations                                      | HTTP and other remote schemes are unsupported; referenced files are trusted build input                               | [External-reference Maven fixture](https://github.com/evryfs/asyncapi-generator/tree/main/asyncapi-generator-maven-plugin/src/test/it/external-reference-scenarios)                                                            |
| Validation                   | Parsed AsyncAPI 3.0 models and supported schema formats                                 | Specification, generator-capability, and advisory findings with paths and source locations      | Validation covers supported workflows rather than every possible extension or downstream runtime                      | [Validation rules](../validator/rules.md)                                                                                                                                                                                      |
| Bundled documents            | Supported parsed models containing local external references                            | Self-contained AsyncAPI YAML or JSON                                                            | Recursive schemas are promoted to local components; bundling does not normalize the contract for generation           | [Bundled document examples](../bundler/examples.md)                                                                                                                                                                            |
| Kotlin models                | JSON-compatible AsyncAPI Schema Object payloads                                         | Kotlin data classes, enums, and supported polymorphic types with Jakarta Validation annotations | Native Avro and Protobuf payloads use their dedicated generators                                                      | [Approved Kotlin model](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/generator/kotlin/simple-transaction-model.approved.kt)                                     |
| Java classes                 | JSON-compatible AsyncAPI Schema Object payloads                                         | Java POJOs, enums, and supported type hierarchies with Jakarta Validation annotations           | Conditional schemas become a safe static property view rather than runtime branching                                  | [Approved Java class](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/generator/java/simple-transaction-class.approved.java)                                       |
| Java records                 | JSON-compatible AsyncAPI Schema Object payloads                                         | Immutable Java records with supported validation and polymorphism                               | Selected explicitly; only available for Java model generation                                                         | [Approved Java record](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/generator/java/simple-transaction-record.approved.java)                                     |
| Spring Kafka contracts       | Channels containing JSON-compatible, native Avro, or native Protobuf message payloads   | Kotlin or Java producer and consumer interfaces with topic, key, and header contracts           | The application owns implementations, serializers, converters, brokers, consumer groups, retries, and transactions    | [Approved Spring Kafka contracts](https://github.com/evryfs/asyncapi-generator/tree/main/asyncapi-generator-core/src/test/resources/approvals/generator/spring-kafka)                                                          |
| Additional producer payloads | Payload-bearing Spring Kafka producer messages                                          | Optional `ByteArray`/`byte[]` and `String` methods in addition to the contract-derived method   | Values must already be serialized; payload validation applies only to the contract-derived method                     | [Approved additional payload methods](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/generator/spring-kafka/kotlin/additional-payload-types-producer.approved.kt) |
| Avro projection              | JSON-compatible AsyncAPI Schema Objects                                                 | Projected `.avsc` record and enum schemas                                                       | This is a projection, not native Avro passthrough; only supported schema shapes and logical-type mappings are emitted | [Approved Avro projection](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/generator/avro/task-schema.approved.avsc)                                               |
| Native Avro                  | Payload schemas using supported Avro 1.9 `schemaFormat` values                          | Native `.avsc` artifacts and optional Java `SpecificRecord` sources                             | Application-owned Kafka serializers remain required                                                                   | [Approved native Avro output](https://github.com/evryfs/asyncapi-generator/tree/main/asyncapi-generator-core/src/test/resources/approvals/generator/native-avro)                                                               |
| Native Protobuf              | Payload schemas using Protobuf 2 or 3 `schemaFormat` values                             | Native `.proto` artifacts and optional Java message sources or Kotlin DSL APIs                  | Java package/type information and `java_multiple_files` are required for generated message types                      | [Approved native Protobuf schema](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/generator/native-protobuf/schema/user-created.approved.proto)                    |
| JSON Schema                  | AsyncAPI Schema Objects and native JSON Schema Draft 7 schemas                          | Standalone Draft 7 `.schema.json` artifacts                                                     | Generated references point to generated schema files; this is not a newer JSON Schema dialect conversion              | [Approved JSON Schema](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/generator/json-schema/schema/my-account.approved.json)                                      |
| Maven plugin                 | Supported core configuration in Maven builds                                            | Generated sources, resources, or bundled documents integrated with the Maven lifecycle          | Domain behavior and defaults are owned by core                                                                        | [Maven integration fixtures](https://github.com/evryfs/asyncapi-generator/tree/main/asyncapi-generator-maven-plugin/src/test/it)                                                                                               |
| Gradle plugin                | Supported core configuration in Gradle builds                                           | Generated output with source-set and task-input integration                                     | Domain behavior and defaults are owned by core                                                                        | [Gradle plugin tests](https://github.com/evryfs/asyncapi-generator/tree/main/asyncapi-generator-gradle-plugin/src/test)                                                                                                        |
| CLI                          | Supported core configuration from command-line options                                  | Direct generation or bundling with process diagnostics                                          | It does not add capabilities beyond core                                                                              | [Packaged CLI integration test](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-cli/src/test/kotlin/dev/banking/asyncapi/generator/cli/CliPackagedApplicationIT.kt)                                  |

## Input and reference boundary

The implemented parser profile supports the AsyncAPI 3.0 specification line,
including patch releases. The declared version is preserved in the model.
AsyncAPI 3.1 documents are rejected with a specific unsupported-profile
diagnostic rather than being interpreted as 3.0 documents.

External references may select complete AsyncAPI documents or supported object
fragments from local files. Selected fragments can come from heterogeneous
documents when the selected value can be interpreted as the required AsyncAPI
object category. Unrelated content in the surrounding file is not imported.

See the [parser contract](../parser/parser-contract.md) for syntax, safety-limit,
source-location, and reference details.

## Generation boundary

Source generation uses a normalized static view where needed for JVM and Avro
projection output. Standalone JSON Schema output retains declared schema
semantics instead. Native Avro and Protobuf payloads remain separate from
AsyncAPI Schema Objects so their dedicated generators can preserve their native
formats.

Every configured output is planned and checked for compatibility before files
are written. Rendering and document serialization complete in memory first,
then all destinations are checked for collisions. A requested capability that
cannot represent the input fails explicitly instead of being skipped.

AsyncAPI Operation Objects are preserved contract data. They do not decide
which producer or consumer interfaces are generated; client output is selected
through generator configuration and derived from channel messages.

## Deliberate non-goals

The current product surface does not provide:

- AsyncAPI 3.1 parsing.
- Remote HTTP reference loading.
- Universal AsyncAPI, JSON Schema, protocol-binding, or framework coverage.
- Spring Kafka implementations or application runtime configuration.
- Kafka serializer, deserializer, converter, broker, or deployment setup.
- Micronaut Kafka, Quarkus Kafka, or framework-neutral Kafka contracts.
- Automatic migration between schema dialects or native schema formats.

Unsupported contract data may still be preserved when it is valid but not
consumed by a requested generator. Unsupported requested output is reported at
the generation compatibility boundary.

## Frontend consistency

The Maven plugin, Gradle plugin, and CLI map their public options into the same
typed core configuration. Defaults, supported finite values, compatibility
checks, and generation behavior therefore belong to core rather than being
reimplemented by each frontend.

The frontends differ only in build-tool integration and presentation. Consult
their configuration documentation when selecting concrete option names; a
complete typed configuration reference is maintained as separate documentation
work.
