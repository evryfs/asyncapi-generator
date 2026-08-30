# Getting started

This tutorial walks you through generating Kotlin data classes from an AsyncAPI specification in under 5 minutes.

## Prerequisites

- Java 21 or later
- Maven 3.9+ or Gradle 8+

## 1. Create an AsyncAPI specification

Create `src/main/resources/asyncapi.yaml`:

```yaml
asyncapi: '3.0.0'
info:
  title: User Events
  version: '1.0.0'
channels:
  userEvents:
    address: user.events
    messages:
      userSignedUp:
        payload:
          $ref: '#/components/schemas/UserSignedUp'
components:
  schemas:
    UserSignedUp:
      type: object
      required:
        - userId
        - email
      properties:
        userId:
          type: string
        email:
          type: string
          format: email
        createdAt:
          type: string
          format: date-time
```

## 2. Configure the generator

Replace `VERSION` with the published version shown on the
[Maven plugin artifact page](https://central.sonatype.com/artifact/dev.banking.asyncapi.generator/asyncapi-generator-maven-plugin)
for Maven or the
[Gradle plugin release page](https://plugins.gradle.org/plugin/dev.banking.asyncapi.generator)
for Gradle.

### Maven

Add to your `pom.xml`:

```xml
<plugin>
    <groupId>dev.banking.asyncapi.generator</groupId>
    <artifactId>asyncapi-generator-maven-plugin</artifactId>
    <version>VERSION</version>
    <executions>
        <execution>
            <id>generate-kotlin-models</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <generatorName>kotlin</generatorName>
                <inputSpec>${project.basedir}/src/main/resources/asyncapi.yaml</inputSpec>
                <modelPackage>com.example.events.model</modelPackage>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Gradle

Add to your `build.gradle.kts`:

```kotlin
plugins {
    id("dev.banking.asyncapi.generator") version "VERSION"
}

asyncApiGenerator {
    executions {
        register("models") {
            generatorName.set("kotlin")
            inputSpec.set(file("src/main/resources/asyncapi.yaml"))
            modelPackage.set("com.example.events.model")
        }
    }
}
```

## 3. Run the generator

### Maven

```sh
mvn generate-sources
```

### Gradle

```sh
./gradlew generateAsyncApi
```

## 4. Check the output

The generator creates `UserSignedUp.kt` at `target/generated-sources/asyncapi/com/example/events/model/UserSignedUp.kt` with Maven or `build/generated/asyncapi/models/com/example/events/model/UserSignedUp.kt` with Gradle:

```kotlin
package com.example.events.model

import java.time.OffsetDateTime

data class UserSignedUp(
    val userId: String,
    val email: String,
    val createdAt: OffsetDateTime? = null,
)
```

## Next steps

- [Generate Spring Kafka clients](../how-to/generate-spring-kafka-clients.md)
- [Generate Avro schemas](../how-to/generate-avro-schemas.md)
- [Bundle multi-file documents](../how-to/bundle-multi-file-documents.md)
