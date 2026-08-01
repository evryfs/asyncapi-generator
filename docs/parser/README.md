# Parser documentation

The parser documentation is organized by reader need. Each page has one
Diataxis role so architectural reasoning, API facts, and procedural guidance do
not become mixed together.

## Explanation

- [Parser architecture](architecture.md) explains the stage boundaries, why
  source-located document nodes form the reader/parser boundary, and where
  references and validation fit.

## Reference

- [Parser contract](parser-contract.md) defines the observable loading contract,
  reader and cursor APIs, null semantics, diagnostics, reference behavior,
  Schema Object behavior, and free-form values.

## How-to guides

- [Change the parser safely](changing-the-parser.md) describes how to place a
  change in the correct stage, implement it, test it, and verify the repository.

There is no parser tutorial yet. A future tutorial should walk through a small
end-to-end parser addition without becoming the canonical API reference or the
place where architectural decisions are recorded.
