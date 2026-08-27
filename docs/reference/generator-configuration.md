# Generator configuration

The generator accepts one generation request at a time. Each request selects
exactly one `generatorName`, which resolves to one of three profiles:

- a source profile for Java or Kotlin source generation;
- a schema profile for schema artifacts without runtime models; or
- a document profile for a bundled AsyncAPI document.

The profile defines which output categories are compatible with the request.
Package fields and `outputFile` activate those categories. A configured request
must activate at least one output; selecting a source profile alone does not
generate anything.

Maven, Gradle, and the CLI expose the public names documented on this page.
They map those values to an internal `GeneratorConfigurationRequest`, whose
shape is different: it contains typed profiles and nested model, schema, and
client requests, and separates source, Java-source, and resource output
directories. The internal object shape is not frontend configuration syntax.

## Common frontend fields

| Field             | Requirement                                                                               | Meaning                                                                                                                                                                                                 |
|-------------------|-------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `inputSpec`       | Required for every run                                                                    | AsyncAPI YAML or JSON input file. This is a frontend input and is loaded before the typed generator configuration is created.                                                                           |
| `generatorName`   | Required for every run                                                                    | Selects exactly one generator profile. Supported values are listed in [Generator profiles](#generator-profiles).                                                                                        |
| `outputDirectory` | Always resolved by the frontend                                                           | Base directory for generated source and schema files. Each frontend has its own default; see [Output directories](#output-directories).                                                                 |
| `outputFile`      | Required for `asyncapi-yaml` and `asyncapi-json`; optional for source and schema profiles | Activates bundled-document output at the specified file. Source and schema profiles serialize this optional output as YAML.                                                                             |
| `modelPackage`    | Conditional                                                                               | Activates model output for a source profile. It is also required when `modelConfig` or client generation is configured. For native Avro and Protobuf models, it is the generated runtime-model package. |
| `clientPackage`   | Conditional                                                                               | Activates client-contract output for a source profile when `clientConfig` is also configured.                                                                                                           |
| `schemaPackage`   | Required for schema profiles; optional for native Avro or Protobuf models                 | Package path for schema artifacts. It is invalid when the selected outputs do not generate schemas.                                                                                                     |

`modelConfig` and `clientConfig` refine activated outputs; neither is an output
category by itself. Model configuration is covered below. Client configuration
is documented separately in the Spring Kafka section of this reference.

## Generator profiles

| `generatorName`   | Profile  | Resulting artifacts                                                                                                                                                                                                                                               | Required output activation                                                                                   | Incompatible configuration categories                                                                                                  |
|-------------------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `java`            | Source   | Java model or client source. Native model types can also produce Avro or Protobuf schemas and their Java runtime models. `outputFile` additionally writes a bundled YAML document.                                                                                | At least one compatible output through `modelPackage`, `clientPackage` with `clientConfig`, or `outputFile`. | `schemaPackage` unless native Avro or Protobuf model output is selected.                                                               |
| `kotlin`          | Source   | Kotlin model or client source. Native Protobuf models include Java messages and Kotlin DSL source; native Avro models are Java SpecificRecords. Native model types also produce their schema artifacts. `outputFile` additionally writes a bundled YAML document. | At least one compatible output through `modelPackage`, `clientPackage` with `clientConfig`, or `outputFile`. | `schemaPackage` unless native Avro or Protobuf model output is selected.                                                               |
| `avro-schema`     | Schema   | Avro schema files for supported AsyncAPI Schema Object projections and native Avro payloads. It does not generate SpecificRecord source. `outputFile` additionally writes a bundled YAML document.                                                                | `schemaPackage`.                                                                                             | Model and client configuration. Additional schema-generation options are not accepted because the profile already selects Avro.        |
| `protobuf-schema` | Schema   | Protobuf schema files for native Protobuf payloads, without Java messages or Kotlin DSL source. `outputFile` additionally writes a bundled YAML document.                                                                                                         | `schemaPackage`.                                                                                             | Model and client configuration. Additional schema-generation options are not accepted because the profile already selects Protobuf.    |
| `json-schema`     | Schema   | JSON Schema files for supported payload schemas, without runtime model source. `outputFile` additionally writes a bundled YAML document.                                                                                                                          | `schemaPackage`.                                                                                             | Model and client configuration. Additional schema-generation options are not accepted because the profile already selects JSON Schema. |
| `asyncapi-yaml`   | Document | One bundled AsyncAPI YAML document.                                                                                                                                                                                                                               | `outputFile`.                                                                                                | Model, client, and schema configuration, including their package fields.                                                               |
| `asyncapi-json`   | Document | One bundled AsyncAPI JSON document.                                                                                                                                                                                                                               | `outputFile`.                                                                                                | Model, client, and schema configuration, including their package fields.                                                               |

The schema profiles select the schema format themselves. They do not accept a
separate schema-type setting. The document profiles are bundle-only profiles;
`outputDirectory` does not activate source or schema output for them.

## Model configuration

`modelPackage` activates model generation for the `java` and `kotlin` source
profiles. When `modelConfig.modelType` is omitted, the selected source profile
provides the default. Setting any `modelConfig` field without `modelPackage` is
invalid.

### Model types

| `modelConfig.modelType` |       `java` |     `kotlin` | Generated runtime model source                                                                                            |
|-------------------------|-------------:|-------------:|---------------------------------------------------------------------------------------------------------------------------|
| `kotlin-data-class`     |           No | Yes, default | Kotlin data class.                                                                                                        |
| `java-class`            | Yes, default |           No | Java class.                                                                                                               |
| `java-record`           |          Yes |           No | Java record.                                                                                                              |
| `avro-specific-record`  |          Yes |          Yes | Java Avro SpecificRecord source generated from native Avro payload schemas.                                               |
| `protobuf-message`      |          Yes |          Yes | `java` produces Java Protobuf messages. `kotlin` produces the required Java messages plus the official Kotlin DSL source. |

The compatibility matrix is exact: Java source generation accepts
`java-class`, `java-record`, `avro-specific-record`, and `protobuf-message`;
Kotlin source generation accepts `kotlin-data-class`,
`avro-specific-record`, and `protobuf-message`.

### Model annotations

`modelConfig.modelAnnotation` adds one annotation to each generated regular JVM
model. Its value must be a fully qualified type name, for example
`com.example.GeneratedPayload`. It is supported only by
`kotlin-data-class`, `java-class`, and `java-record`. It is not supported by
`avro-specific-record` or `protobuf-message`, whose source is owned by the
native schema compiler.

### Native Avro and Protobuf models

`avro-specific-record` and `protobuf-message` are model choices on the `java`
and `kotlin` source profiles; they are not schema-only profiles. Both require
`modelPackage`, generate the native schema artifact, and generate runtime model
source:

- Avro SpecificRecords are Java source for either source profile.
- Protobuf with `generatorName=java` produces Java message source.
- Protobuf with `generatorName=kotlin` produces Java message source and Kotlin
  DSL source. Applications still use the generated Java messages as the
  Protobuf runtime types.

For these two model types, `schemaPackage` is optional and controls the package
path used for emitted native schema files. It does not replace
`modelPackage`, which remains required and controls runtime-model generation.
For regular data-class, class, or record models, `schemaPackage` is not a
compatible output activation.

The `avro-schema` and `protobuf-schema` profiles are different: they require
`schemaPackage` and emit schemas without runtime model source.

## Output directories

The typed core configuration distinguishes its main source directory, Java
source directory, and resource directory. The current Maven, Gradle, and CLI
frontends expose one `outputDirectory` and map all three internal directories to
that location. Package names create subdirectories below it. `outputFile` is a
separate file path and is not implicitly placed below `outputDirectory`.

| Frontend | `outputDirectory` default                                                                              | Integration behavior                                                                                                                    |
|----------|--------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| Maven    | `${project.build.directory}/generated-sources/asyncapi` (normally `target/generated-sources/asyncapi`) | Each plugin execution uses its resolved directory for generated source and schema resources.                                            |
| Gradle   | `build/generated/asyncapi/<execution-name>`                                                            | Each named execution has its own directory. Generated JVM source and schema resources are registered with the project by artifact type. |
| CLI      | `./generated/asyncapi`                                                                                 | The CLI writes source and schema files below this directory but does not configure a build tool's source or resource sets.              |

For frontend syntax and build-tool setup, see the [Maven how-to](../how-to/maven.md),
[Gradle how-to](../how-to/gradle.md), and [CLI how-to](../how-to/cli.md).
