# Reader contract

This reference defines the reader-stage contract in `asyncapi-generator-core`.

## Entry point

```kotlin
val document = DocumentReaderRegistry.read(file)
```

`DocumentReaderRegistry.read(file)` is the reader-stage entry point. It:

1. Detects the format from the file extension (`.yaml`/`.yml` → YAML, `.json` → JSON).
2. Reads the content within `DocumentReaderLimits.DEFAULT` byte limits.
3. Creates a `DocumentSource` with the file's identity and content.
4. Delegates to `YamlDocumentReader` or `JsonDocumentReader`.
5. Returns an `InputDocument` containing the source and root node tree.

Unsupported extensions throw `DocumentReadException.UnsupportedFormat`. Read failures throw `DocumentReadException.UnreadableDocument`.

## Output types

### `InputDocument`

```kotlin
internal data class InputDocument(
    val source: DocumentSource,
    val root: DocumentNode,
)
```

The immutable product of the reader stage. The `root` is a `DocumentNode` tree that the parser consumes.

### `DocumentNode` hierarchy

| Node type | Represents |
|-----------|-----------|
| `DocumentObject` | Object with named members in document order |
| `DocumentArray` | Ordered array of elements |
| `DocumentString` | String scalar |
| `DocumentNumber` | Numeric scalar (same `Number` type for YAML and JSON) |
| `DocumentBoolean` | Boolean scalar |
| `DocumentNull` | Explicit null (distinct from absent member) |

Every node carries a `SourceLocation` with:
- `sourceId` — stable identity of the input source
- `file` — source file for diagnostics and reference resolution
- `path` — dot-separated path within the source (e.g., `root.channels.userEvents`)
- `line` — one-based source line
- `column` — one-based source column

### `DocumentObject` members

Object members retain both key and value locations:

```kotlin
internal data class DocumentMember(
    val keyLocation: SourceLocation,
    val value: DocumentNode,
)
```

This allows validators to distinguish between a missing key and a key with an invalid value.

## Resource limits

| Parameter | Default | Description |
|-----------|---------|-------------|
| `maxDocumentBytes` | 20 MiB | Maximum file size in bytes |
| `maxDocumentCharacters` | 20 MiB | Maximum content length in characters |
| `maxNestingDepth` | 100 | Maximum object/array nesting depth |
| `maxAliasesForCollections` | 50 | Maximum YAML alias references per collection |
| `maxNumberCharacters` | 1,000 | Maximum length of a numeric token |

Limits are enforced before parsing. A document that exceeds any limit throws `DocumentReadException.ResourceLimitExceeded`.

## Error types

| Exception | When |
|-----------|------|
| `UnsupportedFormat` | File extension is not `.yaml`, `.yml`, or `.json` |
| `UnreadableDocument` | File cannot be read (I/O or security error) |
| `ResourceLimitExceeded` | Document exceeds a configured resource limit |
| `MalformedDocument` | Content contains invalid UTF-8 or unmappable characters |
