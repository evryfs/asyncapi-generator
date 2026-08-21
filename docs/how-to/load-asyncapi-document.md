# Load an AsyncAPI document

This guide shows how to load, validate, and access an AsyncAPI document programmatically.

## Prerequisites

Add `asyncapi-generator-core` as a dependency:

```xml
<dependency>
    <groupId>dev.banking.asyncapi.generator</groupId>
    <artifactId>asyncapi-generator-core</artifactId>
    <version>${asyncapi-generator.version}</version>
</dependency>
```

## Loading a document

```kotlin
import dev.banking.asyncapi.generator.core.loader.AsyncApiDocumentLoader
import java.io.File

val loader = AsyncApiDocumentLoader()
val result = loader.load(File("src/main/resources/asyncapi.yaml"))
```

## Accessing the result

```kotlin
val document = result.document      // AsyncApiDocument domain model
val warnings = result.warnings      // List of validation warnings
val sourceFiles = result.sourceFiles // Set of all loaded files (for incremental builds)
```

## Handling warnings

Warnings are validation findings that do not prevent generation but may indicate issues:

```kotlin
if (result.warnings.isNotEmpty()) {
    println(result.formatWarnings())
}
```

`formatWarnings()` returns an empty string when no warnings exist, or a formatted string with source-aware snippets for each warning.

## Error handling

The loader throws exceptions for:
- Unsupported file formats (non-YAML/JSON)
- Unreadable files
- Resource limit violations (oversized documents, deep nesting)
- Malformed content (invalid UTF-8)
- Parser errors (invalid structure)
- Validation errors (semantic issues)

```kotlin
try {
    val result = loader.load(file)
    // use result.document
} catch (e: Exception) {
    // handle error
}
```

## Source file tracking

`sourceFiles` contains every file read during loading, including external references and native schema assets. Use this for incremental build tracking:

```kotlin
result.sourceFiles.forEach { file ->
    println("Loaded: ${file.absolutePath}")
}
```
