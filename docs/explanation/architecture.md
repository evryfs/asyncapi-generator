# How the generation pipeline works

`asyncapi-generator` uses a compiler-style pipeline. The AsyncAPI contract
provides the input data, configuration selects requested outputs, and each
stage either produces a complete result for the next stage or reports a
failure at its own boundary.

## Pipeline

```text
Read YAML or JSON
        -> parse the supported AsyncAPI model and load local references
        -> validate contract semantics
        -> bundle supported references into a self-contained model
        -> plan outputs and check target compatibility
        -> render every requested artifact
        -> preflight destinations and write outputs
```

## Read YAML or JSON

The reader accepts UTF-8 `.yaml`, `.yml`, and `.json` files. It converts either
syntax into the same neutral document representation while retaining source
locations for objects, members, arrays, and scalar values. Syntax checks and
safety limits belong here, before AsyncAPI meaning is applied.

Malformed syntax, unsupported extensions, unreadable files, duplicate keys,
and resource-limit violations stop the pipeline. See
[Load an AsyncAPI document](../how-to/load-asyncapi-document.md) for the public
loading API.

## Parse and load local references

The parser interprets the neutral document as the supported AsyncAPI `3.0.x`
profile and constructs the domain model without coercing malformed values.
Required members, supported object structure, and runtime value types are
checked at this boundary.

Internal references and supported local external references are resolved with
JSON Pointer semantics. Complete external AsyncAPI documents select their own
supported profile; selected raw fragments inherit the profile of the reference
that loads them. External native schema assets are loaded when required. HTTP
and other remote reference schemes are not supported.

Parser failures identify the source file, path, line, and column where the
problem was found.

## Validate contract semantics

Semantic validation checks the parsed model, including reference integrity and
supported generator-wide contract rules. It does not rewrite the contract.
Errors stop the pipeline, while warnings are returned for review without
preventing generation.

Rules that depend on one requested output target are not applied here. A valid
contract remains valid contract data even when a later target cannot represent
it. The [validation rule inventory](../reference/validation-rules.md) lists the
current validation findings.

## Bundle the contract

Bundling transforms the parsed model into a self-contained document model.
Supported external objects are inlined, recursive external schemas are
promoted to local components, and internal references are retained where they
preserve the contract topology.

Only selected external content enters the bundled model. Unrelated surrounding
content from a foreign document is not imported. See
[Bundle multi-file documents](../how-to/bundle-multi-file-documents.md).

## Plan outputs and check compatibility

Configuration selects a source, schema, or document profile and activates its
outputs. The generator turns that request into a finite set of work and checks
whether the bundled contract can be represented by every selected target.

This is where target-specific restrictions belong. A compatibility failure is
reported explicitly instead of dropping contract data or producing an unsafe
approximation. See [Generator configuration](../reference/generator-configuration.md)
for valid profiles and combinations.

## Render every artifact

Compatible planned outputs are rendered as source files, schema artifacts,
Spring Kafka contracts, or bundled documents. Rendering completes before the
filesystem write stage begins, so a rendering failure does not leave earlier
rendered artifacts in their destinations.

The [generated output examples](../reference/generated-output-examples.md) link
complete fixture-backed artifacts for representative workflows.

## Preflight destinations and write outputs

Before writing, the generator resolves each destination and rejects multiple
artifacts that would target the same normalized path. It then stages rendered
content and commits it to the configured source, resource, and document
locations. Destination failures identify the affected artifact and path.

Use [Troubleshooting](../how-to/troubleshooting.md) to identify which boundary
reported a failure and the configuration or contract input to correct.

## Stable boundaries

- The reader owns syntax, source locations, and safety limits.
- The parser and external loader own supported structure and local reference
  resolution.
- Semantic validation owns contract-wide findings over the parsed model.
- Bundling owns the self-contained representation.
- Planning and compatibility checks own requested target restrictions.
- Rendering owns artifact content; writing owns filesystem destinations.

These boundaries keep public behavior stable without making internal class or
factory relationships part of the documentation contract.
