# asyncapi-generator

The asyncapi-generator is an open-source tool for generating code and schemas from AsyncAPI YAML or JSON specifications.

The project is currently in BETA.

## Supported generators

- Kotlin - Data classes with Jakarta Validation annotations from AsyncAPI Schema Object payloads
- Java - POJOs or records with Jakarta Validation annotations from AsyncAPI Schema Object payloads
- Spring Kafka - Client source artifacts for JSON-compatible payload models, native Avro message payloads, and native Protobuf message payloads in both Kotlin and Java
- Avro Projection - `.avsc` schema generation from AsyncAPI Schema Object payloads
- Native Avro - `.avsc` schema artifacts and Apache Avro Java `SpecificRecord` sources from native Avro `schemaFormat` payloads
- Native Protobuf - `.proto` schema artifacts and Java Protobuf message sources from native Protobuf `schemaFormat` payloads

The current documentation provided is still a draft, found in `docs/` folder at the repository root.

## Usage

The asyncapi-generator BETA version is available as a Maven plugin (Maven Central) and as a Gradle plugin (Gradle Plugin Portal / Maven Central).

### Maven

Example usage in your `pom.xml`:

```xml
<plugin>
    <groupId>dev.banking.asyncapi.generator</groupId>
    <artifactId>asyncapi-generator-maven-plugin</artifactId>
    <version>0.0.1</version> <!-- current BETA version -->
    <executions>
        <execution>
            <id>generate-example</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <generatorName>kotlin</generatorName> <!-- options: kotlin, java - default kotlin -->
                <inputFile>path/to/my/asyncapi_specification.yaml</inputFile>
                <models>
                    <packageName>my.package.path.model</packageName>
                </models>
                <clients>
                    <springKafka>
                        <packageName>my.package.path.client</packageName>
                        <!-- Optional: defaults to models.packageName when models are configured -->
                        <modelPackageName>my.package.path.model</modelPackageName>
                    </springKafka>
                </clients>
                <schemas>
                    <avroProjection>
                        <packageName>my.package.path.schema</packageName>
                    </avroProjection>
                </schemas>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Gradle

The Gradle plugin is published to the [Gradle Plugin Portal](https://plugins.gradle.org/) (and Maven Central). The `gradlePluginPortal()` repository is included by default in `settings.gradle.kts`.

`build.gradle.kts`:

```kotlin
plugins {
    id("dev.banking.asyncapi.generator") version "0.0.1" // current BETA version
}

asyncapiGenerate {
    inputFile.set(file("src/main/resources/asyncapi.yaml"))
    generatorName.set("kotlin") // options: kotlin, java - default kotlin
    models {
        packageName.set("my.package.path.model")
    }
    clients {
        springKafka {
            packageName.set("my.package.path.client")
            // Optional: defaults to models.packageName when models are configured
            modelPackageName.set("my.package.path.model")
        }
    }
    schemas {
        avroProjection {
            packageName.set("my.package.path.schema")
        }
    }
}

tasks.named("compileKotlin") {
    dependsOn("generateAsyncApi")
}
```

Or Groovy DSL (`build.gradle`):

```groovy
plugins {
    id 'dev.banking.asyncapi.generator' version '0.0.1'
}

asyncapiGenerate {
    inputFile = file('src/main/resources/asyncapi.yaml')
    generatorName = 'kotlin'
    models {
        packageName = 'my.package.path.model'
    }
    clients {
        springKafka {
            packageName = 'my.package.path.client'
            // Optional: defaults to models.packageName when models are configured
            modelPackageName = 'my.package.path.model'
        }
    }
    schemas {
        avroProjection {
            packageName = 'my.package.path.schema'
        }
    }
}

tasks.named('compileJava') {
    dependsOn 'generateAsyncApi'
}
```

The plugin wires generated sources into the `main` source set automatically when the `java`/`kotlin` plugin is also applied. Run with:

```sh
./gradlew generateAsyncApi
```

### Java Model Type

Java model generation defaults to regular classes. For projects that prefer immutable constructor-based payload models, configure `models.javaModelType` as `record`.

Maven:

```xml
<configuration>
    <generatorName>java</generatorName>
    <inputFile>path/to/my/asyncapi_specification.yaml</inputFile>
    <models>
        <packageName>my.package.path.model</packageName>
        <javaModelType>record</javaModelType> <!-- options: class, record - default class -->
    </models>
</configuration>
```

Gradle Kotlin DSL:

```kotlin
asyncapiGenerate {
    inputFile.set(file("src/main/resources/asyncapi.yaml"))
    generatorName.set("java")
    models {
        packageName.set("my.package.path.model")
        javaModelType.set("record") // options: class, record - default class
    }
}
```

Gradle Groovy DSL:

```groovy
asyncapiGenerate {
    inputFile = file('src/main/resources/asyncapi.yaml')
    generatorName = 'java'
    models {
        packageName = 'my.package.path.model'
        javaModelType = 'record' // options: class, record - default class
    }
}
```

The same option is available from the CLI as `--models-java-model-type record`. Java records are only supported when `generatorName` is `java`.

### Native Avro Generation

Native Avro generation is configured under `schemas.nativeAvro`. It consumes AsyncAPI schemas that use a native Avro `schemaFormat`, writes `.avsc` files to the resource output directory, and can generate Apache Avro Java `SpecificRecord` sources.

Maven:

```xml
<configuration>
    <inputFile>path/to/my/asyncapi_specification.yaml</inputFile>
    <javaSourceOutputDirectory>${project.build.directory}/generated-sources/asyncapi-java</javaSourceOutputDirectory>
    <resourceOutputDirectory>${project.build.directory}/generated-resources/asyncapi</resourceOutputDirectory>
    <schemas>
        <nativeAvro>
            <generateSpecificRecords>true</generateSpecificRecords> <!-- default true when nativeAvro is configured -->
        </nativeAvro>
    </schemas>
</configuration>
```

Gradle Kotlin DSL:

```kotlin
asyncapiGenerate {
    inputFile.set(file("src/main/resources/asyncapi.yaml"))
    schemas {
        nativeAvro {
            enabled.set(true)
            generateSpecificRecords.set(true) // default true when nativeAvro is enabled
        }
    }
}
```

Gradle Groovy DSL:

```groovy
asyncapiGenerate {
    inputFile = file('src/main/resources/asyncapi.yaml')
    schemas {
        nativeAvro {
            enabled = true
            generateSpecificRecords = true // default true when nativeAvro is enabled
        }
    }
}
```

CLI:

```sh
asyncapi-generator \
  --input src/main/resources/asyncapi.yaml \
  --schemas-native-avro \
  --schemas-native-avro-generate-specific-records true
```

Use `generateSpecificRecords = false` or `--schemas-native-avro-generate-specific-records false` when only `.avsc` artifacts should be generated.

### Native Protobuf Generation

Native Protobuf generation is configured under `schemas.nativeProtobuf`. It consumes AsyncAPI schemas that use a native Protobuf `schemaFormat`, writes `.proto` files to the resource output directory, and generates Java Protobuf message sources by default.

Maven:

```xml
<configuration>
    <inputFile>path/to/my/asyncapi_specification.yaml</inputFile>
    <resourceOutputDirectory>${project.build.directory}/generated-resources/asyncapi</resourceOutputDirectory>
    <schemas>
        <nativeProtobuf>
            <enabled>true</enabled>
            <generateJavaMessageTypes>true</generateJavaMessageTypes>
        </nativeProtobuf>
    </schemas>
</configuration>
```

Gradle Kotlin DSL:

```kotlin
asyncapiGenerate {
    inputFile.set(file("src/main/resources/asyncapi.yaml"))
    schemas {
        nativeProtobuf {
            enabled.set(true)
            generateJavaMessageTypes.set(true)
        }
    }
}
```

Gradle Groovy DSL:

```groovy
asyncapiGenerate {
    inputFile = file('src/main/resources/asyncapi.yaml')
    schemas {
        nativeProtobuf {
            enabled = true
            generateJavaMessageTypes = true
        }
    }
}
```

CLI:

```sh
asyncapi-generator \
  --input src/main/resources/asyncapi.yaml \
  --schemas-native-protobuf \
  --schemas-native-protobuf-generate-java-message-types true
```

Use `generateJavaMessageTypes = false` or `--schemas-native-protobuf-generate-java-message-types false` when only `.proto` artifacts should be generated.

Generated Java Protobuf message sources are produced by running `protoc` during generation. The `.proto` schema must declare a Java package or Protobuf package, enable `option java_multiple_files = true;`, and contain a top-level message that matches the payload name. Generated Java Protobuf sources require `protobuf-java` on the consuming project's compile classpath. The generator does not configure Protobuf serializers or deserializers yet; applications still own Kafka runtime wiring.

### Spring Kafka Clients

Spring Kafka output is configured under `clients.springKafka`.

Generated Spring Kafka clients use `models.packageName` for payload model types by default. If models are generated elsewhere, configure `clients.springKafka.modelPackageName` to point the client API at that package without generating model output in the same execution.

For native Avro message payloads, generated Spring Kafka clients use the Java type declared by the Avro schema namespace and name. For example, a native Avro schema with `namespace: com.example.avro` and `name: UserCreated` is used as `com.example.avro.UserCreated` in generated producer and consumer APIs.

For native Protobuf message payloads, generated Spring Kafka clients use the Java type declared by `option java_package`, or by the Protobuf `package` when `java_package` is omitted. Protobuf client generation requires `option java_multiple_files = true;` so the generated message can be referenced as a top-level Java type. The `.proto` schema must contain a top-level message matching the payload name.

The generator does not configure Kafka Avro or Protobuf serializers and deserializers yet; applications still own that runtime wiring.

Generated Spring Kafka clients are contract-only source artifacts. Producer-oriented channels generate producer wrappers around application-provided `KafkaTemplate` instances. Consumer-oriented channels generate consumer interfaces that receive typed `ConsumerRecord` values. The generator does not create Spring Boot auto-configuration, `@KafkaListener` classes, listener containers, serializer configuration, deserializer configuration, or schema registry configuration.

The generated output depends on the channel direction from the AsyncAPI operations. Producer-oriented channels generate producer artifacts. Consumer-oriented channels generate consumer artifacts. When the channel direction is not declared, the generator treats the channel as both producer and consumer.

The Spring Kafka client surface is still being redesigned for the next major version. The generated artifacts should currently be treated as a source-generation contract, not as final application architecture guidance.

### Payload Schema Formats

The generator separates JSON-compatible AsyncAPI payloads from native or explicit multi-format payload schemas.

The default payload path is the AsyncAPI Schema Object. This is the JSON-compatible schema shape used by model generation, Spring Kafka client generation, and Avro Projection. In this mode, a schema is written directly as an AsyncAPI schema:

```yaml
components:
  schemas:
    UserCreated:
      type: object
      required:
        - userId
      properties:
        userId:
          type: string
```

Avro Projection is an explicit projection from this AsyncAPI Schema Object shape into `.avsc` files. It does not consume native Avro schemas and it does not generate Avro `SpecificRecord` classes.

Native schema formats are represented with `schemaFormat`, for example Avro or Protobuf. Native Avro generation consumes Avro schemas directly:

```yaml
components:
  schemas:
    UserCreated:
      schemaFormat: application/vnd.apache.avro+json;version=1.9.0
      schema:
        type: record
        name: UserCreated
        namespace: com.example.avro
        fields:
          - name: userId
            type: string
          - name: email
            type: string
```

The reader and parser recognize known `schemaFormat` values and preserve those schemas as multi-format payloads. Model generation and Avro Projection reject those payloads with a clear generator error because they consume AsyncAPI Schema Object payloads only.

Native Avro generation writes `.avsc` artifacts to the configured resource output directory. When `generateSpecificRecords` is enabled, it also generates Apache Avro Java `SpecificRecord` sources. Maven writes those Java sources to `javaSourceOutputDirectory`, which defaults to a sibling `asyncapi-java` generated-source directory. CLI and Gradle write those Java sources under the Java source root inside the configured codegen output directory.

Native Avro schemas can also be kept in external local `.avsc` files and referenced from the AsyncAPI document. The reference is resolved relative to the AsyncAPI file that owns the schema reference:

```yaml
components:
  schemas:
    UserCreated:
      schemaFormat: application/vnd.apache.avro+json;version=1.9.0
      schema:
        $ref: schemas/UserCreated.avsc
```

Native Protobuf generation consumes `.proto` text directly:

```yaml
components:
  schemas:
    UserCreated:
      schemaFormat: application/vnd.google.protobuf;version=3
      schema: |
        syntax = "proto3";

        package com.example.protobuf;

        option java_package = "com.example.protobuf";
        option java_multiple_files = true;

        message UserCreated {
          string user_id = 1;
          string email = 2;
        }
```

Native Protobuf generation writes `.proto` artifacts to the configured resource output directory. When Java message generation is enabled, it also writes generated Java Protobuf message sources to the Java source output directory. Maven writes those Java sources to `javaSourceOutputDirectory`, which defaults to a sibling `asyncapi-java` generated-source directory. CLI and Gradle write those Java sources under the Java source root inside the configured codegen output directory.

When the `.proto` content declares a `package`, that package is used as the `.proto` output path. For example, `package com.example.protobuf;` is written under `com/example/protobuf`. When `option java_package` is declared, generated Java message sources use that Java package. When `option java_package` is omitted, generated Java message sources use the Protobuf `package`.

Native Protobuf schemas can also be kept in external local `.proto` files and referenced from the AsyncAPI document:

```yaml
components:
  schemas:
    UserCreated:
      schemaFormat: application/vnd.google.protobuf;version=3
      schema:
        $ref: schemas/user-created.proto
```

Spring Kafka client generation supports native Avro and native Protobuf message payloads by referencing their generated Java types. If native Avro or Protobuf artifacts are not generated in the same execution, the referenced classes must already be available to the consuming project.

## Features supported 

### Parser

The core parsing logic is stable and handles the structural validation of AsyncAPI documents.

- [x] **AsyncAPI YAML Support:** Reads and parses YAML format.
- [x] **AsyncAPI JSON Support:** Reads and parses JSON format.
- [x] **Context-Aware Error Handling:** Provides precise error messages with line numbers and JSON paths.
- [x] **Reference Resolution:** Supports internal and external file references, including local native schema asset references.
- [x] **Components:** Full parsing support for Schemas, Messages, Channels, Parameters, etc.

### Schema Formats

- [x] **AsyncAPI Schema Object:** Fully supported for model, Spring Kafka, and Avro Projection outputs.
- [x] **Known Multi-Format Schemas:** Known `schemaFormat` values are recognized and preserved separately from AsyncAPI Schema Object payloads.
- [x] **Native Avro Generation:** Native Avro `.avsc` artifacts and Java `SpecificRecord` sources can be generated from Avro `schemaFormat` payloads.
- [x] **Native Protobuf Generation:** Native Protobuf `.proto` artifacts and Java Protobuf message sources can be generated from Protobuf `schemaFormat` payloads.
- [ ] **Other Multi-Format Outputs:** JSON Schema, OpenAPI, RAML, and other schema families are not yet consumed by generator outputs.

### Validator

- [x] **Structural Validation:** Ensures the AsyncAPI document adheres to the AsyncAPI specification.
- [x] **Context-Aware Error Handling:** Provides precise error messages with line numbers and context.
- [x] **Warnings:** Provides warnings for best practices and potential issues.
- [ ] **Formatted Warnings:** Enhanced warning messages with suggestions for improvement.

## Future plans

- [ ] **Stable Release:** Move from BETA to stable release with comprehensive testing.
- [ ] **CLI Tool:** Publish the already made CLI module to package managers like brew and dnf.
- [ ] **Documentation:** Complete documentation with examples and guides.
- [ ] **Additional Generators:** Expand support for more programming languages and frameworks, i.e., Quarkus Kafka.
- [ ] **Enhanced Schema Support:** Dedicated native schema generation capabilities for JSON Schema, OpenAPI, RAML, and other schema families.
- [ ] **Serialization:** Consider using kotlinx-serialization for writing bundled schemas to files.
