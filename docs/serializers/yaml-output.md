# YAML Output Contract

The generator serializes the parsed AsyncAPI domain model with Jackson's standard YAML serializer configuration.

Generated YAML preserves serialized values; it is not a textual replay of the input document.

- Comments, anchors, original quoting, and original collection style are not preserved by the parsed domain model.
- Unambiguous single-line strings are emitted without quotes for readability and interoperability.
- Strings that a YAML reader could resolve as another scalar type, such as booleans, nulls, numbers, or timestamps, remain quoted.
- Multiline strings are emitted in literal block style (`|`) for readability.
- Strings beginning with `|`, `>`, `'`, or `"` are treated as ordinary string content.
- List flow/block style and surrounding whitespace formatting are not part of a stable API contract.
- Null entries within collections remain null, while null-valued object properties are omitted.

Callers should rely on parsed semantics and explicit model values, not on the exact textual style of the originating document.
