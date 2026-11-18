# Parser

## What it does

The parser turns the document tree of the root contract into one complete, linked model: the AsyncAPI document plus every external document and fragment its references point at, with every `$ref` linked to its target. It records where each model came from, so later phases can point diagnostics at a line in the right file.

## Contract

- **Input:** The root `InputDocument` from the [reader](reader.md).
- **Output:** An `AsyncApiDocument`, the documents and fragments loaded for its references, and the per-load `AsyncApiContext` that tracks sources, node locations, models and reference links.
- **Invariants:**
  - The model is built only from the document tree; the parser never re-reads text.
  - Every model is registered with the node it was built from.
  - After loading, every reference has a link: resolved, unresolved, a cycle, or a target of the wrong kind. Linking never reports; the validator turns link problems into findings.
  - Each external document is read once and each fragment is parsed once per kind and parser profile, however many references reach it.

## Architecture

A load runs four steps in order: **parse → load → link → validate**. The first three belong to the parser.

**Parse.** `AsyncApiParser` maps the root object to an `AsyncApiDocument`. `AsyncApiSpecificationLine` selects the parser profile from the declared `asyncapi` version, and the profile defines the allowed members of every fixed-field object. One parser per object kind maps nodes to models. They share the helpers in `parser.node`: `parseObject` checks an object's members, builds its model and registers it; `parseReferenceable` parses either a `$ref` or an inline object; `parseMap` and `parseList` parse patterned maps and arrays. Every `$ref` is recorded in `ParsedReferences` together with how to parse its target, so a reference to a channel is later parsed as a channel and a binding reference as a binding of its protocol.

**Load.** `ExternalReferenceLoader` follows the recorded references depth first, in the order the parser met them. A reference into another file reads that file. A target in an AsyncAPI document parses the whole document once; any other target is parsed as the kind of object its reference expects. References found in a loaded target are followed before the next one. `SourceBudget` counts documents, targets, depth and bytes across the whole load.

**Link.** `ReferenceLinker` follows every reference to the model it points at and sets `Reference.model` on each reference of the chain. It returns a `ReferenceLink` that says where the chain led. The bundler and the generator rely on every reference being linked.

**Tracking.** `SourceTracking` holds the documents of the load and renders source snippets. `NodeLocations` holds the location of every node by `NodeAddress`. `ModelTracking` maps each model to the node it was built from, so a validator can find the line of a model or of one of its fields.

**Diagnostics.** Parse and load failures are `ParserDiagnostic` values, thrown as `AsyncApiParseException.ParserDiagnosticFailure` with a message rendered by `ParserDiagnosticFormatter`. The [parser contract](../reference/parser-contract.md) lists every diagnostic.

## What it does not do

The parser does not apply semantic rules: whether a reference points at the right kind of object, whether a schema is supported, or whether names are valid is decided by the validator. It does not bundle references or generate code.

## Known limitations

- Only the AsyncAPI 3.0 parser profile is implemented. 3.1 documents are recognized and rejected with a diagnostic.
- References to remote (`http:`) documents are not supported.
- A load stops at its first parse or load failure. All parse and load failures are reported before any validation finding.
