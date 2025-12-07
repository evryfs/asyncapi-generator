# ParserNode Deep Dive

The `ParserNode` class is the fundamental building block of the `asyncapi-generator-core` parsing logic. It serves as a robust abstraction layer over the raw YAML/JSON data structure, providing type safety, path tracking, and consistent error handling.

## Core Responsibilities

1.  **Traversal:** Navigating the document tree (`mandatory`, `optional`, `extractNodes`).
2.  **Type Coercion:** Safely converting raw values to Kotlin types (`coerce<T>`).
3.  **Context Tracking:** Maintaining the current JSON path and reference to the `AsyncApiContext`.
4.  **Normalization:** Handling loose YAML typing (e.g., "true" string as Boolean).

## Key Methods

### `mandatory(nodeKey: String): ParserNode`
- **Purpose:** Retrieves a child node that *must* exist.
- **Behavior:** If the node is missing, it immediately throws `AsyncApiParseException.Mandatory`, which includes the full path to the missing field.
- **Usage:**
  ```kotlin
  val asyncApiVersion = rootNode.mandatory("asyncapi").coerce<String>()
  ```
- **Implementation Details:**
  ```kotlin
  fun mandatory(nodeKey: String): ParserNode {
      // Step 1: Assert the current node (this.node) is a Map.
      val currentNodeMap = node as? Map<*, *>
          ?: throw AsyncApiParseException.Mandatory(
              name = nodeKey,
              path = path,
              context = context
          )
      // Step 2: Retrieve the value associated with 'nodeKey'.
      val childNode = currentNodeMap[nodeKey]
          ?: throw AsyncApiParseException.Mandatory(
              name = nodeKey,
              path = "$path.$nodeKey",
              context = context
          )
      // Step 3: Return a new ParserNode representing the extracted child.
      return ParserNode(
          name = nodeKey,
          node = childRawValue,
          path = "$path.$nodeKey",
          context = context
      )
  }
  ```

### `optional(nodeKey: String): ParserNode?`
- **Purpose:** Retrieves a child node that might not exist.
- **Behavior:** Returns `null` if the node is missing.
- **Usage:**
  ```kotlin
  val id = rootNode.optional("id")?.coerce<String>()
  ```
- **Implementation Details:**
  ```kotlin
  fun optional(nodeKey: String): ParserNode? {
      // Step 1: Check if the current node is a Map.
      val currentNodeMap = node as? Map<*, *>
          ?: return null
      // Step 2: Attempt to retrieve the value.
      val childNode = currentNodeMap[nodeKey]
          ?: return null
      // Step 3: Return a new ParserNode.
      return ParserNode(
          name = nodeKey,
          node = childRawValue,
          path = "$path.$nodeKey",
          context = context
      )
  }
  ```

### `coerce<T>()`
- **Purpose:** Converts the node's value to the specified type `T`.
- **Supported Types:** `String`, `Boolean`, `Number`, `List`, `Map`, `Any`.
- **Behavior:** Throws `AsyncApiParseException.UnexpectedValue` if the value cannot be converted to the expected type.
- **Usage:**
  ```kotlin
  val isRequired = node.coerce<Boolean>()
  ```
- **Architectural Decision:**
  It uses **Reified Generics** (`inline fun <reified T>`) to access the actual type `T` at runtime. This allows for domain-specific exception handling (`AsyncApiParseException.InvalidValue`) instead of generic `ClassCastException`. This "Fail Fast" approach prevents "Ghost Objects" (objects with all null fields) by enforcing structural validity before data extraction.

### `extractNodes(): List<ParserNode>`
- **Purpose:** Iterates over a List or Map node and returns a list of `ParserNode` instances for each child.
- **Usage:** Useful for parsing arrays or maps of objects (e.g., `channels`, `schemas`).
- **Implementation Details:**
  1.  **Map Handling:** Iterates entries, filters for string keys, creates `ParserNode` with dot notation path.
  2.  **List Handling:** Iterates items, creates `ParserNode` with array index notation path (e.g., `tags[0]`).
  3.  **Error Handling:** Throws exception if node is not a container.

### `startsWith(prefix: String): ParserNode?`
- **Purpose:** utility for parsing extension properties (e.g., `x-`) or grouping properties.
- **Implementation Details:**
  1.  **Container Check:** Verifies current node is a Map.
  2.  **Filter:** Selects keys starting with `prefix`.
  3.  **Normalize:** Recursively normalizes values.
  4.  **Return:** New `ParserNode` containing only the filtered entries, or `null` if empty.

## Normalization Logic

The `normalize` method (internal to `ParserNode`) attempts to be helpful by converting compliant strings into their strongly-typed counterparts.

- `"true"` / `"false"` (case-insensitive) -> `Boolean`
- String parsable as Int -> `Int`
- String parsable as Double -> `Double`

**Note:** This behavior allows for some leniency in the input YAML but requires awareness when strict type adherence is desired.

## Error Handling

`ParserNode` is designed to fail fast and fail informatively. By passing the `AsyncApiContext` and maintaining the `path` string (e.g., `components.schemas.User.properties.email`), any exception thrown from within `ParserNode` contains precise location data, which the top-level error handler uses to print user-friendly error messages.
