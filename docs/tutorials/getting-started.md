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

=== "Maven"

    Add to your `pom.xml`:

    ```xml
    <plugin>
        <groupId>dev.banking.asyncapi.generator</groupId>
        <artifactId>asyncapi-generator-maven-plugin</artifactId>
        <version>0.0.1</version>
        <executions>
            <execution>
                <id>generate</id>
                <phase>generate-sources</phase>
                <goals><goal>generate</goal></goals>
                <configuration>
                    <generatorName>kotlin</generatorName>
                    <inputSpec>src/main/resources/asyncapi.yaml</inputSpec>
                    <models>
                        <packageName>com.example.events.model</packageName>
                    </models>
                </configuration>
            </execution>
        </executions>
    </plugin>
    ```

=== "Gradle"

    Add to your `build.gradle.kts`:

    ```kotlin
    plugins {
        id("dev.banking.asyncapi.generator") version "0.0.1"
    }

    asyncapiGenerate {
        inputFile.set(file("src/main/resources/asyncapi.yaml"))
        generatorName.set("kotlin")
        models {
            packageName.set("com.example.events.model")
        }
    }
    ```

## 3. Run the generator

=== "Maven"

    ```sh
    mvn generate-sources
    ```

=== "Gradle"

    ```sh
    ./gradlew generateAsyncApi
    ```

## 4. Check the output

The generator creates `UserSignedUp.kt` in `target/generated-sources/asyncapi` (Maven) or `build/generated/asyncapi` (Gradle):

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
