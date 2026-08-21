# Architecture overview

AsyncAPI Generator processes specifications through a five-stage pipeline. Each stage has a clear boundary and produces output that the next stage consumes.

## Pipeline

```
File → Reader → Parser → Validator → Bundler → Generator → Output
```

### 1. Reader

Converts YAML/JSON files into a format-independent document tree with source locations. Enforces resource limits. See [Reader](reader.md).

### 2. Parser

Interprets the document tree as AsyncAPI structures. Builds a typed domain model with references, multi-format schemas, and external document tracking. See [Parser](parser.md).

### 3. Validator

Checks the parsed model for semantic correctness. Reports errors (must fix) and warnings (should review). Never modifies the model. See [Validator](validator.md).

### 4. Bundler

Resolves external references and produces a self-contained document. Useful for distribution, archival, and tools that need a single-file input. See [Bundler](bundler.md).

### 5. Generator

Converts the validated model into source code, schema files, and client contracts. Multiple generator capabilities can run in parallel. See [Generator](generator.md).

## Design principles

- **Each stage owns its boundary.** The reader does not interpret AsyncAPI semantics. The parser does not validate business rules. The validator does not modify the model.
- **Errors surface early.** Invalid input is rejected at the earliest stage that can detect it.
- **Output is deterministic.** Given the same input and configuration, the generator produces identical output.
- **Stages are independently testable.** Each stage has its own test suite that exercises its contract without depending on other stages.
