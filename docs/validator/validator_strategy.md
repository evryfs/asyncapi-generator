# Validator Strategy

The Validator package ensures that the parsed AsyncAPI document is not just structurally correct (which the Parser guarantees), but also *semantically* valid according to the AsyncAPI 3.0 specification and specific generator constraints.

## Architecture

The validation logic follows the same **Recursive Descent** pattern as the Parser and Bundler.

*   **Root:** `AsyncApiValidator`
*   **Context:** `AsyncApiContext` (used heavily for source mapping errors)
*   **Result:** `ValidationResults` (a container for Errors and Warnings)

## Key Principles

1.  **Context-Aware Reporting:**
    Every validation error references the `AsyncApiContext` to retrieve the line number and file path of the invalid node.
    ```kotlin
    results.error(
        "Invalid version...",
        asyncApiContext.getLine(node, node::asyncapi) // <--- Magic here
    )
    ```

2.  **Semantic Rules:**
    We validate rules that cannot be expressed in the Kotlin type system alone.
    *   **Regex Constraints:** Validating `asyncapi` version string (`3.0.0`), URI formats for `id`, MIME types for `contentType`.
    *   **Referential Integrity:** (Planned) Ensuring that a channel defined in `operations` actually exists in `channels`.
    *   **Generator Constraints:** Rules specific to code generation (e.g., "Schema names must be valid Java identifiers").

3.  **Non-Blocking Validation:**
    The validator collects *all* errors found in the document instead of throwing an exception on the first failure. This gives the user a comprehensive report to fix everything at once.

## Validator Hierarchy

*   `AsyncApiValidator` (Entry Point)
    *   `InfoValidator` (Metadata checks)
    *   `ServerValidator` (URL formats, Protocol checks)
    *   `ChannelValidator` (Address formats, Parameter validation)
    *   `OperationValidator` (Action types, Reply validations)
    *   `ComponentValidator` (Reusable component checks)
    *   `SchemaValidator` (Deep schema validation, mostly structural/logical)

## Future Improvements

*   **Deep Reference Validation:** Ensure that `$ref` pointers resolve to the correct *type* of object (e.g., a Channel ref points to a Channel, not a Schema).
*   **AsyncAPI 3.0 Specifics:** Add more rules for Request/Reply patterns and Operation traits.