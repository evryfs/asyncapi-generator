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

## Spring Kafka client configuration

Spring Kafka contract generation is available with the `java` and `kotlin`
source profiles. Configure all four required public values:

- `clientPackage`, which activates client-contract output and names its package;
- `modelPackage`, which names the payload types used by the contracts;
- `clientConfig.clientType=spring-kafka`; and
- `clientConfig.clientContract=interface`.

`spring-kafka` is the only supported `clientType`, and `interface` is the only
supported `clientContract`. Configuring `clientPackage` without `clientConfig`
is invalid. Configuring `clientConfig` without either package is also invalid.

### Producer and consumer contracts

Producer and consumer contract generation are independently configurable. Both
default to enabled when their settings are omitted or their configuration
objects are empty. Set `producer.enabled=false` or `consumer.enabled=false` to
disable one contract category. Disabling both generates no Spring Kafka
contracts. The overall run must still plan another artifact, or aggregate
generation fails because it produced no artifacts.

These settings select generated contract categories. AsyncAPI operation actions
do not act as producer or consumer generation switches.

`producer.additionalPayloadTypes` adds producer methods for payloads that an
application has already serialized. It does not replace the producer method for
the typed model. Supported values are:

| Value | Additional producer method payload |
|---|---|
| `byte-array` | A byte-array payload. |
| `string` | A string payload. |

The values are additive. Configuring both produces the typed-model method plus
one byte-array method and one string method. Repeated values are de-duplicated.

### Topic properties and validation annotations

`clientConfig.topicParameterProperties` maps an AsyncAPI channel parameter name
to a Spring property name. For example, `environment` to `kafka.environment`
causes an `{environment}` channel-address parameter to use the Spring property
`kafka.environment`. Values are property names without `${...}` placeholder
syntax. The public shape is a string-to-string map:

```yaml
environment: kafka.environment
```

`clientConfig.validationAnnotations` supports two optional fully qualified type
names:

| Field | Generated annotation target |
|---|---|
| `clientContract` | The generated client interface. |
| `payloadParameter` | Consumer payload parameters and the typed producer payload method. It is not applied to additional byte-array or string producer methods. |

For example, use `org.springframework.validation.annotation.Validated`, not
`Validated`. The generator adds configured annotations to the contracts; it
does not configure validation behavior in the application.

The generated clients are interfaces. The application supplies their
implementations and runtime Spring Kafka wiring. See
[Generate Spring Kafka clients](../how-to/generate-spring-kafka-clients.md) for
operational setup and usage.

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

## Frontend mapping

The three frontends map the following public concepts to the same typed core
request. The table shows their actual configuration shapes; it does not expose
the internal `GeneratorConfigurationRequest` fields.

| Concept                       | Maven XML                                                                                                    | Gradle Kotlin DSL                                                   | CLI                                                    |
|-------------------------------|--------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|--------------------------------------------------------|
| Request boundary              | One `<execution><configuration>`                                                                             | One named `executions.register("name")` block                       | One invocation; no execution-name equivalent           |
| Input specification           | `<inputSpec>...</inputSpec>`                                                                                 | `inputSpec.set(file("..."))`                                        | `--input-spec`, `-i`                                   |
| Generator profile             | `<generatorName>...</generatorName>`                                                                         | `generatorName.set("...")`                                          | `--generator-name`, `-g`                               |
| Output directory              | `<outputDirectory>...</outputDirectory>`                                                                     | `outputDirectory.set(...)`                                          | `--output-directory`, `-o`                             |
| Bundled document file         | `<outputFile>...</outputFile>`                                                                               | `outputFile.set(file("..."))`                                       | `--output-file`                                        |
| Model package                 | `<modelPackage>...</modelPackage>`                                                                           | `modelPackage.set("...")`                                           | `--model-package`                                      |
| Model type                    | `<modelConfig><modelType>...</modelType></modelConfig>`                                                      | Per execution: `modelConfig { modelType.set("...") }`               | `--model-type`; no `modelConfig` container             |
| Model annotation              | `<modelConfig><modelAnnotation>...</modelAnnotation></modelConfig>`                                          | Per execution: `modelConfig { modelAnnotation.set("...") }`         | `--model-annotation`; no `modelConfig` container       |
| Client package                | `<clientPackage>...</clientPackage>`                                                                         | Per execution: `clientPackage.set("...")`                           | `--client-package`                                     |
| Client type                   | `<clientConfig><clientType>...</clientType></clientConfig>`                                                  | Shared: `clientConfig { clientType.set("...") }`                    | `--client-type`; no `clientConfig` container           |
| Client contract               | `<clientConfig><clientContract>...</clientContract></clientConfig>`                                          | Shared: `clientConfig { clientContract.set("...") }`                | `--client-contract`                                    |
| Producer enablement           | `<producer><enabled>true\|false</enabled></producer>` inside `clientConfig`                                  | Shared: `producer { enabled.set(true\|false) }`                     | `--generate-producer` or `--no-generate-producer`      |
| Additional producer payloads  | `<producer><additionalPayloadTypes><additionalPayloadType>byte-array</additionalPayloadType></additionalPayloadTypes></producer>` inside `clientConfig` | Shared: `producer { additionalPayloadTypes.set(listOf(...)) }`      | Repeat `--producer-additional-payload-type VALUE`      |
| Consumer enablement           | `<consumer><enabled>true\|false</enabled></consumer>` inside `clientConfig`                                  | Shared: `consumer { enabled.set(true\|false) }`                     | `--generate-consumer` or `--no-generate-consumer`      |
| Topic parameter properties    | `<topicParameterProperties><PARAMETER>PROPERTY</PARAMETER></topicParameterProperties>` inside `clientConfig` | Shared map: `topicParameterProperties.put("PARAMETER", "PROPERTY")` | Repeat `--topic-parameter-property PARAMETER=PROPERTY` |
| Client validation annotation  | `<validationAnnotations><clientContract>...</clientContract></validationAnnotations>`                        | Shared: `validationAnnotations { clientContract.set("...") }`       | `--client-contract-validation-annotation`              |
| Payload validation annotation | `<validationAnnotations><payloadParameter>...</payloadParameter></validationAnnotations>`                    | Shared: `validationAnnotations { payloadParameter.set("...") }`     | `--payload-parameter-validation-annotation`            |
| Schema package                | `<schemaPackage>...</schemaPackage>`                                                                         | `schemaPackage.set("...")`                                          | `--schema-package`                                     |

Maven fields are scoped to a plugin execution. Maven's normal plugin
configuration merging can provide shared values outside an execution and let
execution configuration specialize them.

Gradle places `modelConfig` and package fields on each named execution, but
places `clientConfig` directly under `asyncApiGenerator` and shares it across
all client-generating executions. Each execution still maps to a distinct
generation request.

The CLI has no named-execution, shared-configuration, `modelConfig`, or
`clientConfig` container equivalent. Its individual flags form one generation
request per invocation. Repeatable additional-payload and topic-property flags
assemble the corresponding list and map.

## Equivalent Spring Kafka scenario

The following configurations describe the same request: Kotlin data-class
models in `com.example.model` and Spring Kafka producer and consumer interfaces
in `com.example.client`. The model type and producer/consumer settings are
omitted so their Kotlin and enabled defaults apply. Each frontend uses its own
default `outputDirectory`.

### Maven

```xml
<plugin>
    <groupId>dev.banking.asyncapi.generator</groupId>
    <artifactId>asyncapi-generator-maven-plugin</artifactId>
    <version>0.3.4-BETA</version>
    <executions>
        <execution>
            <id>generate-client</id>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <generatorName>kotlin</generatorName>
                <inputSpec>${project.basedir}/src/main/resources/asyncapi.yaml</inputSpec>
                <modelPackage>com.example.model</modelPackage>
                <clientPackage>com.example.client</clientPackage>
                <clientConfig>
                    <clientType>spring-kafka</clientType>
                    <clientContract>interface</clientContract>
                </clientConfig>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Gradle Kotlin DSL

```kotlin
plugins {
    id("dev.banking.asyncapi.generator") version "0.3.4-BETA"
}

asyncApiGenerator {
    clientConfig {
        clientType.set("spring-kafka")
        clientContract.set("interface")
    }
    executions {
        register("client") {
            generatorName.set("kotlin")
            inputSpec.set(file("src/main/resources/asyncapi.yaml"))
            modelPackage.set("com.example.model")
            clientPackage.set("com.example.client")
        }
    }
}
```

### CLI

```sh
asyncapi-generator \
  --input-spec src/main/resources/asyncapi.yaml \
  --generator-name kotlin \
  --model-package com.example.model \
  --client-package com.example.client \
  --client-type spring-kafka \
  --client-contract interface
```

For complete frontend setup and commands, see the [Maven how-to](../how-to/maven.md),
[Gradle how-to](../how-to/gradle.md), and [CLI how-to](../how-to/cli.md).
