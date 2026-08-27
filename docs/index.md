# AsyncAPI Generator

AsyncAPI Generator creates Kotlin and Java payload models, Spring Kafka client
contracts, Avro schemas, and Protobuf artifacts from AsyncAPI 3 specifications.

## Quick start

### Maven

```xml
<plugin>
    <groupId>dev.banking.asyncapi.generator</groupId>
    <artifactId>asyncapi-generator-maven-plugin</artifactId>
    <version>0.3.4-BETA</version>
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
                <modelPackage>com.example.model</modelPackage>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Gradle

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

## What it generates

| Generator           | Output                                               |
|---------------------|------------------------------------------------------|
| **Kotlin**          | Data classes with Jakarta Validation annotations     |
| **Java**            | POJOs or records with Jakarta Validation annotations |
| **Spring Kafka**    | Producer and consumer client contracts               |
| **Avro Projection** | `.avsc` schema files from AsyncAPI Schema Objects    |
| **Native Avro**     | `.avsc` files and `SpecificRecord` Java sources      |
| **Native Protobuf** | `.proto` files and Java message sources              |
| **JSON Schema**     | Standalone Draft 7 `.schema.json` files              |

## Learn more

- [Tutorials](tutorials/getting-started.md) — get started in 5 minutes
- [How-to guides](how-to/load-asyncapi-document.md) — accomplish specific tasks
- [Reference](reference/reader-contract.md) — detailed API and contract documentation
- [Explanation](explanation/architecture.md) — understand the design and architecture

## Suggest an improvement

Use [GitHub Issues](https://github.com/evryfs/asyncapi-generator/issues) to report
a problem, request a capability, or suggest a documentation improvement.
