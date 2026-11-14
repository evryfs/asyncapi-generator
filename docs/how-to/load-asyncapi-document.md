# Use the core library

This guide shows how to load, validate, bundle, and generate from an AsyncAPI
document programmatically, for tools that embed the generator instead of
using the Maven plugin, Gradle plugin, or CLI.

## Prerequisites

Add `asyncapi-generator-core` as a dependency:

```xml
<dependency>
    <groupId>dev.banking.asyncapi.generator</groupId>
    <artifactId>asyncapi-generator-core</artifactId>
    <version>${asyncapi-generator.version}</version>
</dependency>
```

The public API is the three phases below and their configuration types. Other
classes in the library are internal and can change without notice.

## Load a document

```kotlin
import dev.banking.asyncapi.generator.core.loader.AsyncApiDocumentLoader
import java.io.File

val loaded = AsyncApiDocumentLoader().load(File("src/main/resources/asyncapi.yaml"))
```

`load` reads the file and every file it references, parses the contract, and
validates it. The result exposes:

```kotlin
val document = loaded.document       // AsyncApiDocument domain model
val warnings = loaded.warnings       // validation warnings
val sourceFiles = loaded.sourceFiles // every file read, for incremental builds
```

`formatWarnings()` returns an empty string when there are no warnings, or the
warnings with source-aware snippets:

```kotlin
if (loaded.warnings.isNotEmpty()) {
    println(loaded.formatWarnings())
}
```

`sourceFiles` contains the root document, every external document, and every
native schema file, as canonical files in path order. Use it as the input set
of an incremental build step.

## Bundle and generate

Generation accepts only a bundled document, so bundle the loaded result
first:

```kotlin
import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import dev.banking.asyncapi.generator.core.generator.AsyncApiGenerator
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationFactory
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationRequest
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName

val bundled = AsyncApiBundler().bundle(loaded)

val outputDirectory = File("build/generated/asyncapi")
val configuration =
    GeneratorConfigurationFactory.create(
        GeneratorConfigurationRequest(
            generatorName = GeneratorName.KOTLIN,
            sourceOutputDirectory = outputDirectory,
            resourceOutputDirectory = outputDirectory,
            models = GeneratorConfigurationRequest.Models(packageName = "com.example.model"),
        ),
    )
configuration.warnings.forEach { warning -> println(warning.message) }

AsyncApiGenerator().generate(bundled, configuration)
```

`GeneratorConfigurationFactory.create` validates the request with the same
rules as the frontends; see
[Generator configuration](../reference/generator-configuration.md).
`bundled.document` is the bundled `AsyncApiDocument` if you need the model
itself.

`AsyncApiGenerator()` resolves `protoc` like the CLI does, from the local
Maven repository or Maven Central. To control it, pass a `ProtocProvider`,
for example `AsyncApiGenerator(MavenRepositoryProtocProvider(override = path))`
for a local executable.

## Handle errors

Every phase throws instead of returning partial results:

| Phase     | Exception                          | Cause                                                                                           |
|-----------|------------------------------------|-------------------------------------------------------------------------------------------------|
| Load      | `DocumentReadException` subclasses | Missing, unreadable, empty, or malformed files, unsupported extensions, and resource limits.     |
| Load      | `AsyncApiParseException`           | Contract structure that does not match the supported AsyncAPI profile, and unreadable native schema files. |
| Load      | `AsyncApiValidateException`        | Validation errors, including unresolved references and reference cycles, with source locations. |
| Bundle    | `AsyncApiBundlingException`        | Schemas from different files that share a name but differ in content.                           |
| Configure | `IllegalArgumentException`         | Invalid or incomplete generator configuration.                                                  |
| Generate  | `AsyncApiGeneratorException`       | Outputs that cannot represent the contract, such as inline schemas that would get the same generated name but describe different types, and destination conflicts. |

The exception messages are formatted for people. For programmatic handling,
use the structured fields described in the
[parser contract](../reference/parser-contract.md#parser-diagnostics) and the
[validation rule inventory](../reference/validation-rules.md).
