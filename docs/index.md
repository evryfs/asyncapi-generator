# asyncapi-generator

`asyncapi-generator` turns supported AsyncAPI `3.0.x` contracts into
deterministic Kotlin and Java models, schema artifacts, Spring Kafka
interfaces, and self-contained bundled documents. It preserves valid contract
data and reports when a requested output cannot represent that data safely.

The implemented parser profile supports AsyncAPI `3.0.x`, including suffixed
versions such as `3.0.0-rc1`. AsyncAPI `3.1.x` and other specification lines are
not supported. See [Supported capabilities](reference/supported-capabilities.md)
for the complete production workflow matrix and current limitations.

## Start here

- Follow [Getting started](tutorials/getting-started.md) for the introductory
  generation workflow.
- Configure the [Maven](how-to/maven.md), [Gradle](how-to/gradle.md), or
  [CLI](how-to/cli.md) frontend.
- Use [Generator configuration](reference/generator-configuration.md) for
  accepted values, defaults, and required combinations.

## Explore the documentation

- [Project purpose and scope](explanation/purpose-and-scope.md) explains the
  supported-workflow focus and runtime boundaries.
- [How the generation pipeline works](explanation/architecture.md) explains the
  stable stages from document reading through artifact writing.
- [Generated output examples](reference/generated-output-examples.md) links
  complete fixture-backed models, contracts, and schema artifacts.
- [Troubleshooting](how-to/troubleshooting.md) maps reported failures to
  actionable input, configuration, compatibility, and destination checks.

## Independent project

This is an independent project, not the
[official AsyncAPI Generator](https://github.com/asyncapi/generator). The name
"AsyncAPI" is used descriptively for compatibility with the
[AsyncAPI specification](https://www.asyncapi.com/docs/reference/specification/v3.0.0).
