# Gradle

## Kotlin DSL

```kotlin
plugins {
    id("dev.banking.asyncapi.generator") version "0.0.1"
}

asyncapiGenerate {
    inputFile.set(file("src/main/resources/asyncapi.yaml"))
    generatorName.set("kotlin")
    models {
        packageName.set("com.example.model")
    }
}

tasks.named("compileKotlin") {
    dependsOn("generateAsyncApi")
}
```

## Groovy DSL

```groovy
plugins {
    id 'dev.banking.asyncapi.generator' version '0.0.1'
}

asyncapiGenerate {
    inputFile = file('src/main/resources/asyncapi.yaml')
    generatorName = 'kotlin'
    models {
        packageName = 'com.example.model'
    }
}

tasks.named('compileKotlin') {
    dependsOn 'generateAsyncApi'
}
```

Run with:

```sh
./gradlew generateAsyncApi
```

## Configuration options

| Option          | Description                      | Default  |
|-----------------|----------------------------------|----------|
| `generatorName` | Generator profile                | required |
| `inputFile`     | AsyncAPI YAML or JSON file       | required |
| `outputFile`    | File for bundled document output | —        |
| `modelPackage`  | Package for generated models     | —        |
| `clientPackage` | Package for generated clients    | —        |
| `schemaPackage` | Package for generated schemas    | —        |

## Model configuration

```kotlin
asyncapiGenerate {
    models {
        packageName.set("com.example.model")
        javaModelType.set("record") // class or record
        annotation.set("com.example.Nullable")
    }
}
```

## Spring Kafka client configuration

```kotlin
asyncapiGenerate {
    clients {
        kafka {
            packageName.set("com.example.client")
            modelPackageName.set("com.example.model")
            springKafka {
                enabled.set(true)
                clientContract.set("interface")
                producer {
                    enabled.set(true)
                    additionalPayloadTypes.set(listOf("byte-array"))
                }
                consumer {
                    enabled.set(true)
                }
            }
        }
    }
}
```

## Schema configuration

```kotlin
asyncapiGenerate {
    schemas {
        avroProjection {
            packageName.set("com.example.schema")
        }
        nativeAvro {
            enabled.set(true)
            generateSpecificRecords.set(true)
        }
        nativeProtobuf {
            enabled.set(true)
        }
    }
}
```
