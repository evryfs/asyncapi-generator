# Architecture overview

AsyncAPI Generator processes specifications through a compiler-style pipeline. Each stage has a clear boundary and produces output that the next stage consumes.

## Pipeline

```text
External AsyncAPI source
          |
          v
Reader
  syntax, normalization, source locations, safety limits
          |
          v
InputDocument
          |
          v
Parser and external reference loading
          |
          v
AsyncApiDocument
          |
          v
Semantic validation
          |
          v
Bundling
          |
          v
Bundled AsyncApiDocument
          |
          v
GenerationInputFactory
          |
          v
GenerationInput
          |
          +-------------------------------+
                                          |
GeneratorConfiguration                    |
          |                               |
          v                               |
GenerationPlanner                         |
          |                               |
          v                               |
GenerationPlan                            |
          |                               |
          +-------------------------------+
                          |
                          v
Generation compatibility validation
                          |
                          v
Target artifact rendering
                          |
                          v
GenerationResult
                          |
                          v
Filesystem artifact writer
```

The contract determines what can be generated. Configuration determines what should be generated. Compatibility validation determines whether those two inputs can be combined. Rendering produces target-specific artifacts. The writer persists the artifacts.

Model, schema, and client backends consume `GenerationInput`. Bundled-document rendering preserves and serializes the bundled `AsyncApiDocument` directly. `GenerationPlan` selects both kinds of work.

## Pipeline stages

### 1. Reader

Converts YAML/JSON files into a format-independent document tree with source locations. Enforces resource limits. See [Reader](reader.md).

### 2. Parser

Interprets the document tree as AsyncAPI structures. Builds a typed domain model with references, multi-format schemas, and external document tracking. See [Parser](parser.md).

### 3. Validator

Checks the parsed model for semantic correctness. Reports errors (must fix) and warnings (should review). Never modifies the model. See [Validator](validator.md).

The validator has two stages: semantic validation and generation compatibility validation. See [Validation boundary](../contributing/validation-boundary.md).

### 4. Bundler

Resolves external references and produces a self-contained document. Useful for distribution, archival, and tools that need a single-file input. See [Bundler](bundler.md).

### 5. Generator

Converts the validated model into source code, schema files, and client contracts. Multiple generator capabilities can run in parallel. See [Generator](generator.md).

The generator has two inputs:
- `GenerationInput` — describes what the contract contains
- `GenerationPlan` — describes what configuration requested

Compatibility validation determines whether the requested outputs can consume the contract.

## Design principles

- **Each stage owns its boundary.** The reader does not interpret AsyncAPI semantics. The parser does not validate business rules. The validator does not modify the model.
- **Errors surface early.** Invalid input is rejected at the earliest stage that can detect it.
- **Output is deterministic.** Given the same input and configuration, the generator produces identical output.
- **Stages are independently testable.** Each stage has its own test suite that exercises its contract without depending on other stages.

## Architecture decision

For the full architectural context, see the [architecture decision record](../../.local/ddd-architecture-analysis.md).
