# Gradle

The Gradle plugin configures one task for each named execution.

## Kotlin DSL

```kotlin
plugins {
    id("dev.banking.asyncapi.generator") version "0.3.4-BETA"
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
    id 'dev.banking.asyncapi.generator' version '0.3.4-BETA'
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

By default, that execution writes generated files below `build/generated/asyncapi/models`.

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

## Generate Spring Kafka clients

`clientConfig` is shared by client-generating executions. Each execution still defines its own model and client packages:

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

The schema-only generator profiles are `avro-schema`, `protobuf-schema`, and `json-schema`.

## Execution fields

| Field             | Description                                                                                                             | Default                                     |
|-------------------|-------------------------------------------------------------------------------------------------------------------------|---------------------------------------------|
| `generatorName`   | Generator profile (`kotlin`, `java`, `avro-schema`, `protobuf-schema`, `json-schema`, `asyncapi-yaml`, `asyncapi-json`) | required                                    |
| `inputSpec`       | AsyncAPI YAML or JSON file                                                                                              | required                                    |
| `outputDirectory` | Directory for generated sources and schemas                                                                             | `build/generated/asyncapi/<execution-name>` |
| `outputFile`      | File for bundled document output                                                                                        | none                                        |
| `modelPackage`    | Package for generated payload models                                                                                    | none                                        |
| `clientPackage`   | Package for generated client contracts                                                                                  | none                                        |
| `schemaPackage`   | Package for generated schema artifacts                                                                                  | none                                        |
| `modelConfig`     | Model implementation and annotation settings for this execution                                                         | none                                        |

`clientConfig` is configured once on `asyncApiGenerator` and is shared by all named executions.
