# Parsing Strategy

The **Parser** is the entry point of the AsyncAPI generation pipeline. Its responsibility is to transform raw YAML input 
into a strongly-typed, traversable **Abstract Syntax Tree (AST)** (represented by the `AsyncApiDocument` model) while 
maintaining strict tracking of source code locations for error reporting.

## Core Architecture

The parsing infrastructure is built around a **Recursive Descent** strategy, supported by a unified Context object.

### 1. Context-Driven Parsing (`AsyncApiContext`)

Unlike traditional parsers that might be stateless functions, our parser is built around an **`AsyncApiContext`**. This instance-based context acts as the "Session State" for a single parsing run.

It holds:
*   **`SourceRepository`:** Stores the raw content of all loaded files (root + external refs) and a map of JSON Pointers to line numbers.
*   **`ModelRepository`:** A registry of every parsed model object instance. This allows us to:
    *   Resolve references (`$ref`) by looking up the target path.
    *   Retrieve the source line number for any model object during Validation.
*   **`AsyncApiExternalLoader`:** A service to recursively load and parse external files into the *same* context.

**Why this matters:** This "Monolithic Context" approach ensures that even if an API definition is split across 100 files, the Validator and Generator see it as a single, unified graph of objects with consistent reference resolution.

### 2. The `ParserNode` Cursor

To navigate the raw YAML structure (provided by SnakeYAML/Jackson), we use a wrapper class called **`ParserNode`**.

*   **Role:** It acts as a cursor pointing to a specific node in the YAML tree.
*   **State:** It carries the `AsyncApiContext` and the current JSON Pointer path (e.g., `root.components.schemas.User`).
*   **Safety:** It provides helper methods for type coercion (`coerce<String>`) and structural enforcement (`mandatory`, `optional`).

**Error Handling:**
When a structural requirement fails (e.g., a missing mandatory field or wrong type), `ParserNode` throws an `AsyncApiParseException`. Because `ParserNode` knows its exact location path and has access to the Context, the exception immediately generates a rich error message with a code snippet.

### 3. Recursive Descent Parsers

The logic is split into specialized parser classes mirroring the AsyncAPI 3.0 structure:

*   `AsyncApiParser` (Root)
    *   `InfoParser`
    *   `ServerParser`
    *   `ChannelParser`
    *   `ComponentParser`
        *   `SchemaParser`
        *   `MessageParser`
        *   ...and so on.

Each parser class:
1.  Accepts `AsyncApiContext` in its constructor.
2.  Implements a `parse(node: ParserNode)` method.
3.  Extracts fields using `node.optional()` or `node.mandatory()`.
4.  Recursively calls sub-parsers for nested objects.
5.  **Registers** the resulting model in the Context (`context.register(model, node)`).

### 4. Reference Handling Strategy

The parser treats Reference Objects (`$ref`) as first-class citizens.

When parsing any component (e.g., a Schema), the parser checks for the presence of `$ref` **first**.
*   **If `$ref` exists:** It creates a `Reference` object wrapping the string string.
    *   Crucially, it **registers** this Reference in the Context.
    *   This registration triggers the `AsyncApiExternalLoader` if the reference points to an external file, ensuring the target is parsed and available before validation begins.
*   **If `$ref` is missing:** It parses the object as an inline definition (e.g., `SchemaInline`).

This "Eager Loading" strategy ensures that by the time `parser.parse()` returns, the entire dependency graph (across all files) is loaded into memory, ready for Validation.

### 5. Schema Dependencies (JSON Schema)

The `SchemaParser` implements specific logic for the `dependencies` keyword to comply with JSON Schema Draft 7:
*   It distinguishes between **Property Dependencies** (List of strings) and **Schema Dependencies** (Map/Schema object).
*   This requires dynamic type checking during parsing to correctly route the node to either simple coercion or recursive schema parsing.

## Summary of Data Flow

1.  **Input:** Raw File (`file.yaml`).
2.  **Registry:** `AsyncApiRegistry.read(file, context)` loads file content into `SourceRepository`.
3.  **Parse:** `AsyncApiParser(context).parse(rootNode)` starts recursion.
4.  **Traverse:** `ParserNode` navigates the tree.
5.  **Register:** Every created model is registered in `ModelRepository` with its line number.
6.  **Load:** External refs trigger recursive loading into the same Context.
7.  **Output:** An `AsyncApiDocument` object (the AST root) + a populated `AsyncApiContext` (the semantic graph).
