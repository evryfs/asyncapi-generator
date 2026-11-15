# Reader

## What it does

The reader converts YAML or JSON files into a format-independent document tree. It selects the correct parser by file extension, enforces resource limits, and preserves source locations so downstream phases can produce accurate diagnostics.

## Contract

- **Input:** A `File` reference to an AsyncAPI YAML (`.yaml`, `.yml`) or JSON (`.json`) document.
- **Output:** An `InputDocument` containing a `DocumentSource` (file identity, content, format) and a `DocumentNode` root tree with source locations on every node.
- **Invariants:**
  - Every node retains its source file, line, and column for diagnostic reporting.
  - Object members retain key locations separately from value locations.
  - Explicit YAML/JSON `null` is represented as `DocumentNull`, distinct from an absent member.
  - Document order is preserved in both object members and array elements.
  - Content is validated against configurable resource limits (max bytes, characters, nesting depth, number length).

## Architecture

The reader stage consists of these parts:

**`SourceReader`** is the entry point. It detects the input format from the file extension, reads the content within the byte limit, creates a `DocumentSource`, and delegates to the reader for that format.

**`DocumentReader`** is implemented by **`YamlDocumentReader`** and **`JsonDocumentReader`**. The interface performs the checks every format shares: document size, byte-order mark, and blank content. Each format only parses its content into a tree of `DocumentNode` values. Each node type — `DocumentObject`, `DocumentArray`, `DocumentString`, `DocumentNumber`, `DocumentBoolean`, `DocumentNull` — maps directly to a JSON or YAML value kind. `DocumentObjectBuilder` collects object members in document order and rejects duplicate keys for both formats, and `YamlScalarReader` converts YAML scalars so that numbers follow JSON syntax.

**`NodePath`** is the position of a value inside one document, such as `root.channels.userSignedUp`, `root.servers[0]`, or `root["channel.name"]`. Every node carries a `SourceLocation` with its file, path, line, and column. **`SourceId`** identifies a document within one load; the parser combines it with a `NodePath` to address a node across all documents of the load.

**`SourceLimits`** holds every resource limit of one load. The readers enforce the limits for each document:

| Limit                   | Default |
|-------------------------|---------|
| Max document bytes      | 20 MiB  |
| Max document characters | 20 MiB  |
| Max nesting depth       | 100     |
| Max collection aliases  | 50      |
| Max number characters   | 1,000   |

**`SourceBudget`** enforces the limits for the whole load, across the root document, its external documents, and native schema assets:

| Limit                        | Default |
|------------------------------|---------|
| Max source documents         | 256     |
| Max reference targets        | 4,096   |
| Max external reference depth | 64      |
| Max aggregate source bytes   | 64 MiB  |
| Max native schema asset size | 20 MiB  |

These limits prevent denial-of-service from oversized, deeply nested, or endlessly referencing inputs.

## What it does not do

The reader does not interpret AsyncAPI semantics. It does not validate schema structure, resolve `$ref` references, or assign domain meaning to object keys. That work belongs to the parser and validator stages.

## Known limitations

- Only `.yaml`, `.yml`, and `.json` extensions are recognized. Other extensions are rejected.
- The reader does not handle streaming or chunked input — the entire document must fit in memory.
- YAML 1.1 vs 1.2 differences are handled by the underlying SnakeYAML parser; the reader does not expose version selection.
