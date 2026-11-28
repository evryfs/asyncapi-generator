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

The API is the package `dev.banking.asyncapi.generator.core`: `AsyncApiGenerator`,
`GeneratorSettings`, and the types they use. Every other package of the library
is internal and can change without notice.

## Generate

```kotlin
import dev.banking.asyncapi.generator.core.AsyncApiGenerator
import dev.banking.asyncapi.generator.core.GeneratorSettings
import java.io.File

val generator = AsyncApiGenerator()
val generation =
    generator.configure(
        GeneratorSettings(
            generatorName = "kotlin",
            outputDirectory = File("build/generated/asyncapi"),
            modelPackage = "com.example.model",
        ),
    )
if (generation.warnings.isNotEmpty()) println(generation.warnings)

generator.generate(File("src/main/resources/asyncapi.yaml"), generation) { warnings -> println(warnings) }
```

`configure` checks the settings without reading the contract or the file
system. `GeneratorSettings` has the same fields as the frontends, checked with
the same rules; see
[Generator configuration](../reference/generator-configuration.md). The result
tells what generation will write: `writesSources` and `writesSchemas` say
whether `outputDirectory` will hold sources to compile or schemas to package,
and `requiresProtoc` whether `protoc` runs.

`generate` first checks that an existing `outputDirectory` is a directory and
that `outputFile` is not one. It then reads the contract and every file it
references, validates and bundles it, and writes the output. Contract warnings,
with source snippets, go to the function you pass.

`inputFiles` returns the root document, every external document, and every
native schema file of a contract, as canonical files in path order. Use it as
the input set of an incremental build step.

`AsyncApiGenerator()` resolves `protoc` like the CLI does, from the local Maven
repository or Maven Central. To control it, pass a `ProtocProvider`, for
example `AsyncApiGenerator(ProtocProvider.executable(path))` for a local
executable.

## Handle errors

Every step throws an `AsyncApiGenerationException` instead of returning partial
results:

| Exception         | Cause |
|-------------------|-------|
| `InvalidSettings` | A missing, invalid, or incomplete setting. Nothing was read or written. |
| `Failed`          | Missing, unreadable, or malformed files; contract structure outside the supported AsyncAPI profile; validation errors with source locations; schemas that cannot be bundled; outputs that cannot represent the contract; and files that cannot be written. |

The messages are formatted for people.
