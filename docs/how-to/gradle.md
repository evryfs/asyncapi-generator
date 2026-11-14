# Gradle

The Gradle plugin configures one task for each named execution.

Replace `VERSION` with the published version shown on the
[Gradle plugin release page](https://plugins.gradle.org/plugin/dev.banking.asyncapi.generator).

## Requirements

The plugin is compiled for Java 21, so Gradle must run on Java 21 or later.
The project's continuous integration tests the plugin with one Gradle version,
`9.5.1`, using both the Kotlin and the Groovy DSL and the configuration cache.
Gradle `8.14.3` has been verified manually once; other Gradle versions are not
tested.

## Kotlin DSL

```kotlin
plugins {
    id("dev.banking.asyncapi.generator") version "VERSION"
}

asyncApiGenerator {
    executions {
        register("models") {
            generatorName.set("kotlin")
            inputSpec.set(file("src/main/resources/asyncapi.yaml"))
            modelPackage.set("com.example.model")
        }
    }
}
```

## Groovy DSL

```groovy
plugins {
    id 'dev.banking.asyncapi.generator' version 'VERSION'
}

asyncApiGenerator {
    executions {
        register('models') {
            generatorName.set('kotlin')
            inputSpec.set(file('src/main/resources/asyncapi.yaml'))
            modelPackage.set('com.example.model')
        }
    }
}
```

Run every configured execution with the aggregate task:

```sh
./gradlew generateAsyncApi
```

Each execution also has its own task. The `models` execution above creates `generateModelsAsyncApi`:

```sh
./gradlew generateModelsAsyncApi
```

By default, that execution writes generated files below
`build/generated/asyncapi/models`. When the `java` or
`org.jetbrains.kotlin.jvm` plugin is applied, the output directory is added to
the `main` source set (Java and Kotlin sources, and schema files as
resources), and compilation depends on the generation task automatically.

## Configure models

`modelConfig` belongs to an individual execution:

```kotlin
asyncApiGenerator {
    executions {
        register("models") {
            generatorName.set("java")
            inputSpec.set(file("src/main/resources/asyncapi.yaml"))
            modelPackage.set("com.example.model")
            modelConfig {
                modelType.set("java-record")
            }
        }
    }
}
```

See [Generate Kotlin and Java models](generate-kotlin-models.md) for the
generated code and its runtime dependencies.

## Generate Spring Kafka clients

`clientConfig` is configured once, directly under `asyncApiGenerator`, and is
shared by all executions. Each execution still defines its own model and
client packages:

```kotlin
asyncApiGenerator {
    clientConfig {
        clientType.set("spring-kafka")
        clientContract.set("interface")
        producer {
            enabled.set(true)
            additionalPayloadTypes.set(listOf("byte-array"))
        }
        consumer {
            enabled.set(true)
        }
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

An execution without `clientPackage` ignores the shared `clientConfig` and
prints the `unused-client-config` warning. Silence it for executions that
intentionally generate no client, either for every execution or for one:

```kotlin
asyncApiGenerator {
    // Silenced for every execution.
    suppressedWarnings.add("unused-client-config")

    executions {
        register("schemas") {
            generatorName.set("avro-schema")
            inputSpec.set(file("src/main/resources/asyncapi.yaml"))
            schemaPackage.set("com.example.schema.avro")
            // Or silenced for this execution only.
            suppressedWarnings.add("unused-client-config")
        }
    }
}
```

An execution silences the union of the top-level and its own list. Unknown
warning ids fail the task. See
[Generate Spring Kafka clients](generate-spring-kafka-clients.md) for every
client setting.

## Generate schemas

Use a schema generator profile and set `schemaPackage` on the execution. This example generates Avro schema files without runtime models:

```kotlin
asyncApiGenerator {
    executions {
        register("schemas") {
            generatorName.set("avro-schema")
            inputSpec.set(file("src/main/resources/asyncapi.yaml"))
            schemaPackage.set("com.example.schema.avro")
        }
    }
}
```

The schema-only generator profiles are `avro-schema`, `protobuf-schema`, and
`json-schema`.

## Resolve protoc for native Protobuf models

Executions with `modelConfig.modelType` `protobuf-message` resolve
`com.google.protobuf:protoc` through the `asyncApiProtoc` configuration from
the project's repositories. Declare a repository that provides it:

```kotlin
repositories {
    mavenCentral()
}
```

Other executions never resolve `protoc`. To use a local executable instead,
set `protocExecutable` on the extension:

```kotlin
asyncApiGenerator {
    protocExecutable.set(file("/opt/protoc/bin/protoc"))
}
```

See [Generate Protobuf schemas and models](generate-protobuf-schemas.md).

## Up-to-date checks and the configuration cache

The generation tasks support the configuration cache. A task's inputs are its
settings, the root contract, and every file the contract reaches through an
external `$ref`, including native schema files. The task is up to date until
one of those inputs changes; editing a referenced file regenerates without
invalidating the configuration cache.

## Stale generated files

The plugin records the files each execution generates in
`build/asyncapi-generator/output-manifest.txt`. When a later run of the same
execution no longer generates a file, for example because a schema was removed
from the contract, the file is deleted, unless another execution still
generates it. Files the plugin did not generate are never deleted, and the
bundled `outputFile` is not managed. See
[Stale generated files](../reference/generator-configuration.md#stale-generated-files).

## Extension and execution fields

Fields on `asyncApiGenerator`:

| Field                | Description                                                                 | Default                                                             |
|----------------------|-----------------------------------------------------------------------------|---------------------------------------------------------------------|
| `clientConfig`       | Spring Kafka client settings shared by all executions                       | none                                                                |
| `suppressedWarnings` | Configuration warning ids silenced for every execution                      | empty                                                               |
| `protocExecutable`   | Local `protoc` for native Protobuf models                                   | `com.google.protobuf:protoc:3.25.5` from the project repositories   |
| `executions`         | Named executions; each one is a generation request with its own task        | none                                                                |

Fields on each execution:

| Field                | Description                                                                                                             | Default                                     |
|----------------------|-------------------------------------------------------------------------------------------------------------------------|---------------------------------------------|
| `generatorName`      | Generator profile (`kotlin`, `java`, `avro-schema`, `protobuf-schema`, `json-schema`, `asyncapi-yaml`, `asyncapi-json`) | required                                    |
| `inputSpec`          | AsyncAPI YAML or JSON file                                                                                              | required                                    |
| `outputDirectory`    | Directory for generated sources and schemas                                                                             | `build/generated/asyncapi/<execution-name>` |
| `outputFile`         | File for bundled document output                                                                                        | none                                        |
| `modelPackage`       | Package for generated payload models                                                                                    | none                                        |
| `clientPackage`      | Package for generated client contracts                                                                                  | none                                        |
| `schemaPackage`      | Package for generated schema artifacts                                                                                  | none                                        |
| `modelConfig`        | Model implementation and annotation settings for this execution                                                         | none                                        |
| `suppressedWarnings` | Configuration warning ids silenced for this execution, in addition to the top-level ones                                | empty                                       |
