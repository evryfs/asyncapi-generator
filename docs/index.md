# AsyncAPI Generator documentation

AsyncAPI Generator creates Kotlin and Java payload models, Spring Kafka client
contracts, Avro schemas, and Protobuf artifacts for supported AsyncAPI 3.0
workflows.

The documentation is organized by the kind of information you need:

- **Reference** defines stable behavior and shows verified examples.
- **Explanation** describes the project's purpose and supported boundaries.
- **Maintainer documentation** records architecture and safe change procedures.

User guides and complete frontend configuration references will be added as the
project prepares for its first stable release. Until then, see the
[project README](https://github.com/evryfs/asyncapi-generator#readme) for current
Maven, Gradle, and CLI usage.

## Start exploring

- Read the [project purpose and scope](explanation/purpose-and-scope.md) to
  understand what the generator owns and deliberately leaves to applications.
- Check the [supported capabilities](reference/supported-capabilities.md) before
  choosing a generation workflow.
- Inspect [bundled document examples](bundler/examples.md) with verified source
  and output documents.
- Use the [validation rule reference](validator/rules.md) to interpret validator
  findings.
- Read the [parser contract](parser/parser-contract.md) for supported document
  loading and reference behavior.

Architecture and change procedures are available under **Maintainer
documentation** for contributors working on the implementation.

## Suggest an improvement

Use [GitHub Issues](https://github.com/evryfs/asyncapi-generator/issues) to report
a problem, request a capability, or suggest a documentation improvement.
