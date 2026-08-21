# Test coverage

Test coverage is generated using [Kover](https://kotlin.github.io/kotlinx-kover/) and published alongside this documentation.

## Generating coverage reports

Run the following from the project root:

```sh
mvn verify -pl asyncapi-generator-core
```

This generates:

- **HTML report:** `asyncapi-generator-core/target/site/kover/index.html`
- **XML report:** `asyncapi-generator-core/target/site/kover/kover.xml`

The HTML report provides browsable per-package and per-file coverage details.

## Interpreting the report

- **Line coverage** — percentage of code lines executed by tests
- **Branch coverage** — percentage of decision branches (if/when) exercised
- **Instructions** — low-level bytecode instruction coverage (most precise)

Focus on line and branch coverage for actionable insights. Instruction coverage is useful for detecting untested edge cases in conditionals.
