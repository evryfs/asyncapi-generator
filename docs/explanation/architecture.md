# How the generation pipeline works

`asyncapi-generator` uses a compiler-style pipeline. The AsyncAPI contract
provides the input data, configuration selects requested outputs, and each
stage either produces a complete result for the next stage or reports a
failure at its own boundary.

## Pipeline

```text
check the input file and the configuration
        -> load: read YAML or JSON, parse the supported AsyncAPI model,
           load local references, validate contract semantics
        -> bundle supported references into a self-contained model
        -> generate: plan outputs, check target compatibility,
           render every requested artifact
        -> preflight destinations, write outputs, remove stale files
```

The three phases are typed. Loading produces a validated document, bundling
turns only such a document into a bundled document, and generation accepts
only a bundled document. A later phase therefore cannot receive input that
skipped an earlier one. Maven, Gradle, and the CLI run the same phases in the
same order; they differ only in how they collect configuration, resolve
`protoc`, and register outputs with a build.

Every frontend checks first that the input file exists, so a missing file is
always the first reported problem. Generation configuration is then validated
before generation starts. Gradle also reads the contract while configuring
the build to discover the files it references, so in Gradle a contract error
can be reported before a configuration error.

## Read YAML or JSON

The reader accepts UTF-8 `.yaml`, `.yml`, and `.json` files. It converts either
syntax into the same neutral document representation while retaining source
locations for objects, members, arrays, and scalar values. Syntax checks and
safety limits belong here, before AsyncAPI meaning is applied.

Malformed syntax, unsupported extensions, unreadable files, duplicate keys,
recursive YAML aliases, and resource-limit violations stop the pipeline. See
[Use the core library](../how-to/load-asyncapi-document.md) for the public
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

Semantic validation checks the parsed model, including reference integrity,
reference chains that never reach an object, and supported generator-wide
contract rules. It does not rewrite the contract.
Errors stop the pipeline, while warnings are returned for review without
preventing generation.

Rules that depend on one requested output target are not applied here. A valid
contract remains valid contract data even when a later target cannot represent
it. The [validation rule inventory](../reference/validation-rules.md) lists the
current validation findings.

## Bundle the contract

Bundling transforms the validated model into a self-contained document model
without reading files or changing the loaded model. Supported external objects
are inlined. References to the root document's component schemas are kept,
and component schemas and recursive schemas from other files are promoted to
the bundle's components under their own names. Because schema names survive
bundling, code generated from a bundled document equals code generated from
the original files.

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

Kotlin and Java models are rendered from one language-neutral model of the
payload types. Decisions that affect the JSON wire format, such as enum
values, property names, and polymorphic type names, are made once in that
model; the Kotlin and Java emitters only choose syntax. Both languages
therefore serialize the same payloads in the same way. Spring Kafka contracts
follow the same split.

The [generated output examples](../reference/generated-output-examples.md) link
complete fixture-backed artifacts for representative workflows.

## Preflight destinations and write outputs

Before writing, the generator resolves each destination and rejects multiple
artifacts that would target the same normalized path. It then stages rendered
content and commits it to the configured source, resource, and document
locations. Files whose content is unchanged are left untouched. Destination
failures identify the affected artifact and path.

Maven and Gradle then remove files that the same execution generated before
but no longer generates, using a manifest of generated files per project.
Files the generator did not write are never removed. See
[Stale generated files](../reference/generator-configuration.md#stale-generated-files).

Use [Troubleshooting](../how-to/troubleshooting.md) to identify which boundary
reported a failure and the configuration or contract input to correct.

## Stable boundaries

- The reader owns syntax, source locations, and safety limits.
- The parser and external loader own supported structure and local reference
  resolution.
- Semantic validation owns contract-wide findings over the parsed model.
- Bundling owns the self-contained representation and is a pure
  transformation.
- Planning and compatibility checks own requested target restrictions.
- The language-neutral model owns generated types and their wire format;
  emitters own language syntax.
- Rendering owns artifact content; writing owns filesystem destinations and
  stale-file removal.

Maintainers can find the internal structure in
[Bundler strategy](bundler.md) and [Generator strategy](generator.md).

These boundaries keep public behavior stable without making internal class or
factory relationships part of the documentation contract.
