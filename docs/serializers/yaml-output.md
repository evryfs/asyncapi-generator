# YAML Output Contract

The generator serializes the parsed AsyncAPI domain model with Jackson's standard YAML serializer configuration.

Generated YAML preserves domain-model values and types, but it is not a textual replay of the input document.

- Comments, anchors, original quoting, and original collection style are not preserved by the parsed domain model.
- Single-line strings may be quoted according to Jackson's default YAML behavior.
- Multiline strings are emitted in literal block style (`|`) for readability.
- Strings beginning with `|`, `>`, `'`, or `"` are treated as ordinary string content.
- List flow/block style and surrounding whitespace formatting are not part of a stable API contract.
- Null properties are omitted from output.

Callers should rely on parsed semantics and explicit model values, not on the exact textual style of the originating document.
