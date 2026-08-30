# Project purpose and scope

`asyncapi-generator` turns supported AsyncAPI `3.0.x` contracts into
deterministic JVM models, schema artifacts, Spring Kafka interfaces, and
self-contained bundled documents. It treats the contract as the source of
truth, preserves valid contract data, and reports explicitly when a requested
target cannot represent that contract safely.

## Purpose

The project supports maintainable JVM generation workflows from one contract:

- Kotlin data classes and Java classes or records;
- Spring Kafka producer and consumer interfaces;
- Avro, Protobuf, and JSON Schema artifacts, including supported native schema
  workflows; and
- bundled YAML or JSON documents for supported multi-file contracts.

The [supported capabilities](../reference/supported-capabilities.md) reference
defines the current input, result, and limitations of each workflow. Complete
fixture-backed artifacts are linked from
[generated output examples](../reference/generated-output-examples.md).

## Scope boundaries

The project favors stable supported workflows over universal AsyncAPI, JSON
Schema, protocol, or framework coverage. A valid optional contract feature is
preserved even when generation does not consume it. If a selected output cannot
represent the contract safely, generation fails at the compatibility boundary
rather than changing or discarding the contract data.

AsyncAPI operations remain contract data. They do not act as producer or
consumer generation directives; Spring Kafka contract generation is selected
from channels and configuration.

Generated interfaces and models are integration artifacts, not application
runtime configuration. The project does not configure brokers, serializers,
Spring beans, listener containers, consumer groups, retries, transactions, or
deployment infrastructure.

## Relationship to AsyncAPI

The project uses "AsyncAPI" descriptively to state compatibility with the
[AsyncAPI specification](https://www.asyncapi.com/docs/reference/specification/v3.0.0).
It is an independent project and is not the
[official AsyncAPI Generator](https://github.com/asyncapi/generator).
