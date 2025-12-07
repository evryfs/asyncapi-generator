# Bundler Strategy

The **Bundler** is responsible for transforming a potentially multi-file AsyncAPI definition (using `$ref` to external files) into a **Single, Self-Contained Document**.

## Core Goal

The Code Generator expects a single `AsyncApiDocument` object where all references are local or resolved. It should not have to worry about file I/O or external loaders. The Bundler provides this abstraction.

## Architecture

The Bundler operates on the **Parsed Model** (`AsyncApiDocument`), not the raw YAML. It traverses the model and "flattens" it.

### Data Flow

1.  **Input:** A parsed `AsyncApiDocument` (which may contain `Reference` objects pointing to external files loaded in `AsyncApiContext`).
2.  **Traversal:** The bundler visits every node (Channels, Messages, Schemas).
3.  **Resolution:**
    *   If it encounters an **Inline** definition, it copies it.
    *   If it encounters a **Reference** (`$ref`):
        1.  It asks the `AsyncApiContext` (implicitly or explicitly via `Reference.ref`) to resolve the target.
        2.  **Strategy Decision:**
            *   **Dereference (Inline):** Replace the `$ref` with the actual object definition. (Used for small objects or cross-file refs).
            *   **Internalize:** Move the referenced object into the `components` section of the root document and rewrite the `$ref` to point to `#/components/schemas/MyMovedObject`. (Preferred for Schemas to keep the document clean).
4.  **Output:** A new `AsyncApiDocument` that is structurally identical but has no dependencies on external files.

### Circular Dependency Handling

The bundler maintains a `visited` set of references during traversal. If it detects a cycle (A -> B -> A), it stops recursing and keeps the `$ref` as-is (or ensures it points to a component definition), preventing infinite loops during bundling.

## Bundler Components

*   `AsyncApiBundler` (Root orchestrator)
*   `SchemaBundler` (Complex logic for flattening schema inheritance and refs)
*   `ChannelBundler`
*   `MessageBundler`
*   ... etc.

## Why Bundle?

1.  **Generator Simplicity:** The Generator logic becomes stateless and file-system agnostic.
2.  **Portability:** The output of the Bundler can be saved as a single `asyncapi.bundled.yaml` file, which is useful for distribution or importing into other tools (like UI documentation renderers).