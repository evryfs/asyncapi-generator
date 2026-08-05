# YAML Output Contract

The generator serializes the parsed AsyncAPI domain model with Jackson's standard YAML serializer configuration.

Generated YAML preserves serialized values; it is not a byte-for-byte replay of input text.

- It preserves strings, numbers, booleans, collection entries, and object members included in the output.
- Null entries within collections remain null, while null-valued object properties are omitted.
- It does not preserve source-document formatting decisions such as comments, anchors, original quoting, or collection style.
- Single-line strings may be quoted according to Jackson's normal output rules.
- Multiline strings are emitted using YAML literal block style (`|`) for readability.
- Strings starting with YAML-indicating characters like `|`, `>`, `'`, or `"` are serialized as normal data values.
- Flow/style differences, list-style formatting, and whitespace layout are not part of the stable output contract.

This means callers should rely on parsed semantics and explicit model values, not on the exact textual style of the originating document.
