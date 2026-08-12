# Purpose and scope

`asyncapi-generator` turns supported AsyncAPI 3.0 contracts into deterministic
JVM models, schema artifacts, Spring Kafka interfaces, and self-contained
bundled documents. It treats the contract as the source of truth, preserves
valid contract data, and reports when a requested output cannot represent that
contract safely.

The project focuses on maintainable production workflows rather than universal
coverage of AsyncAPI, JSON Schema, protocols, or application frameworks.
Support is added when it enables a concrete workflow without weakening the
stability of existing output.

## How a contract becomes output

The stable processing flow is:

```text
Read YAML or JSON
-> construct the supported AsyncAPI model
-> resolve local references
-> validate semantics and generator capabilities
-> plan requested outputs
-> render all artifacts
-> preflight destinations
-> write outputs
```

Each stage has a distinct responsibility. Syntax and document safety belong to
reading and parsing. Semantic conformance belongs to validation. Restrictions
specific to a requested output belong to generation compatibility checks.
Bundling transforms the parsed model into a self-contained document without
turning it into generated source code.

## Contract preservation

AsyncAPI operations, channels, messages, schemas, bindings, and other supported
objects remain contract data even when a generator target does not consume all
of them. The project does not reinterpret operations as producer or consumer
generation instructions. Client generation is selected explicitly through
generator configuration.

Unsupported requested output fails explicitly at the boundary that owns the
restriction. Valid optional contract data is not rejected merely because a
particular generator does not use it.

## Application responsibilities

Generated artifacts express compile-time contracts. The generator does not
configure or operate application infrastructure, including:

- Kafka brokers, producer factories, consumer groups, or deployment resources.
- Application implementations of generated client interfaces.
- Runtime serializers, deserializers, or message converters.
- Dependency injection components or Spring beans.
- Retry, acknowledgement, transaction, or error-handling policies.

Those choices remain application-owned because they depend on runtime and
operational requirements outside the AsyncAPI document.

## AsyncAPI compatibility

The implemented parser profile supports AsyncAPI 3.0 documents. Support for a
newer specification version is not implied automatically; it requires an
explicit parser profile and verification of the affected workflows.

This is an independent open-source project. It uses “AsyncAPI” descriptively
to identify compatibility with the
[AsyncAPI specification](https://www.asyncapi.com/docs/reference/specification/v3.0.0)
and is not the
[official AsyncAPI Generator](https://github.com/asyncapi/generator).
