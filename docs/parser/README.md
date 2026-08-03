# Parser documentation

The parser documentation is organized by reader need. Each page has one
Diataxis role so architectural reasoning, API facts, and procedural guidance do
not become mixed together.

## Explanation

- [Parser architecture](architecture.md) explains the stage boundaries, why
  source-located document nodes form the internal reader/parser boundary, how
  collision-safe identity and load budgets work, and why the loader is the
  supported public API.

## Reference

- [Parser contract](parser-contract.md) defines the observable loading contract,
  reader and cursor behavior, fixed-member and null semantics, diagnostics,
  reference and resource-limit behavior, Schema Object handling, canonical
  source dependencies, and free-form values.

## How-to guides

- [Change the parser safely](changing-the-parser.md) describes how to place a
  change in the correct stage, preserve internal/public boundaries, implement
  it, test it, and verify the repository.

There is no parser tutorial yet. A future tutorial should walk through a small
end-to-end parser addition without becoming the canonical API reference or the
place where architectural decisions are recorded.
