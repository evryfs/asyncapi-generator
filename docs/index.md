# AsyncAPI Generator documentation

AsyncAPI Generator creates Kotlin and Java payload models, Spring Kafka client
contracts, Avro schemas, and Protobuf artifacts for supported AsyncAPI 3.0
workflows.

The documentation is organized by the kind of information you need:

- **Examples** demonstrate complete inputs and generated or bundled results.
- **Reference** defines stable behavior such as the parser contract, validation
  rules, and serialized output.
- **Explanation** describes the architectural boundaries and design decisions.
- **Contributing** explains how to change those boundaries safely.

User guides and complete frontend configuration references will be added as the
project prepares for its first stable release. Until then, see the
[project README](https://github.com/evryfs/asyncapi-generator#readme) for current
Maven, Gradle, and CLI usage.

## Start exploring

- Review the [generator strategy](generator/generator_strategy.md) to understand
  the supported generation boundary.
- Inspect [bundled document examples](bundler/examples.md) with verified source
  and output documents.
- Use the [validation rule reference](validator/rules.md) to interpret validator
  findings.
- Read the [parser contract](parser/parser-contract.md) for supported document
  loading and reference behavior.

## Suggest an improvement

Use [GitHub Issues](https://github.com/evryfs/asyncapi-generator/issues) to report
a problem, request a capability, or suggest a documentation improvement.
