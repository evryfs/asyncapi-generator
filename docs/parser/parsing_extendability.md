# Contributing to the Parser

This guide outlines how to contribute to the `parser` package in `asyncapi-generator-core`.

## Architecture Overview

The parser is organized hierarchically.
- **Root:** `AsyncApiParser`
- **Sub-parsers:** `InfoParser`, `ChannelParser`, `SchemaParser`, etc.
- **Data Structure:** `ParserNode` (wraps raw YAML/JSON)

## Adding a New Parser

If you are adding support for a new section of the AsyncAPI specification (e.g., a new object type in `components`):

1.  **Create the Class:**
    Create a new Kotlin class in the appropriate package (e.g., `com.tietoevry.banking.asyncapi.generator.core.parser.newfeature`).
    ```kotlin
    class NewFeatureParser(
        val asyncApiContext: AsyncApiContext
    ) {
        // ...
    }
    ```

2.  **Implement Parsing Logic:**
    Define a `parse` method (usually `parseElement` or `parseMap`) that takes a `ParserNode`.
    ```kotlin
    fun parseElement(node: ParserNode): NewFeatureModel {
        return NewFeatureModel(
            name = node.mandatory("name").coerce<String>(),
            isEnabled = node.optional("enabled")?.coerce<Boolean>() ?: false
        ).also { asyncApiContext.register(it, node) }
    }
    ```

3.  **Register with Context:**
    Always call `asyncApiContext.register(modelObject, node)` on the created model object. This is crucial for tracking line numbers and source mapping.

## Best Practices

- **Use `ParserNode`:** Never access the raw `Map` or `List` directly. Use `mandatory`, `optional`, and `coerce` to ensure type safety and correct error reporting.
- **Immutability:** Parse into immutable data classes (val properties).
- **Dependency Injection:** Pass `AsyncApiContext` down to all sub-parsers.

## Testing

1.  **Location:** Add tests in `asyncapi-generator-core/src/test/kotlin/...`.
2.  **Style:** Use the existing test infrastructure which allows loading YAML files and asserting the parsed object structure.
3.  **Coverage:** Ensure you test:
    - Valid inputs (all fields).
    - Missing optional fields.
    - Missing mandatory fields (should throw `AsyncApiParseException.Mandatory`).
    - Invalid types (should throw `AsyncApiParseException.UnexpectedValue`).

## Handling "TODOs"

If you encounter a `TODO` in the code:
1.  Analyze if it's a quick fix (e.g., better error handling).
2.  If complex, open an issue or discuss with the maintainers.
3.  Do not leave new `TODO`s without a clear reason or ticket reference.
