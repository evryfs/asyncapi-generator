# Generated output examples

This catalog links complete generated artifacts to the AsyncAPI contracts and
ApprovalTests that produce them. The approved files are the canonical examples;
the full generated source is linked instead of copied into this page.

Configuration values use the public names shared by the Maven, Gradle, and CLI
frontends. See [Generator configuration](generator-configuration.md) for each
frontend's syntax and output-directory behavior.

## JSON-compatible Kotlin DTO

The
[`asyncapi_simple_transaction_type.yaml`](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/generator/asyncapi_simple_transaction_type.yaml)
contract defines a JSON-compatible component Schema Object with required and
optional properties, string formats, and validation constraints.

| Configuration value     | Example setting                                                   |
|-------------------------|-------------------------------------------------------------------|
| `inputSpec`             | `asyncapi_simple_transaction_type.yaml`                           |
| `generatorName`         | `kotlin`                                                          |
| `modelPackage`          | `dev.banking.asyncapi.generator.core.model.generated.transaction` |
| `modelConfig.modelType` | `kotlin-data-class` (the Kotlin default)                          |

The run generates
`dev/banking/asyncapi/generator/core/model/generated/transaction/SimpleTransactionType.kt`.
[`KotlinModelApprovalTest`](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/kotlin/dev/banking/asyncapi/generator/core/generator/kotlin/KotlinModelApprovalTest.kt)
enforces the relationship between the contract and the complete
[`SimpleTransactionType.kt` approved output](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/generator/kotlin/simple-transaction-model.approved.kt).

Required schema properties become non-null constructor parameters. Optional
properties become nullable parameters with a `null` default. Formats and
constraints map to JVM types and Jakarta Validation annotations where supported.
The generated DTO does not configure a JSON mapper; the application must provide
JSON serialization support for types such as `UUID`, `BigDecimal`, `LocalDate`,
and `OffsetDateTime`.

## Java record

The Java record example uses the same
[`asyncapi_simple_transaction_type.yaml`](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/generator/asyncapi_simple_transaction_type.yaml)
contract so the effect of selecting a different source profile and model type is
directly inspectable.

| Configuration value     | Example setting                                                   |
|-------------------------|-------------------------------------------------------------------|
| `inputSpec`             | `asyncapi_simple_transaction_type.yaml`                           |
| `generatorName`         | `java`                                                            |
| `modelPackage`          | `dev.banking.asyncapi.generator.core.model.generated.transaction` |
| `modelConfig.modelType` | `java-record`                                                     |

The run generates
`dev/banking/asyncapi/generator/core/model/generated/transaction/SimpleTransactionType.java`.
The `approves_generated_java_record_model` case in
[`JavaModelApprovalTest`](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/kotlin/dev/banking/asyncapi/generator/core/generator/java/JavaModelApprovalTest.kt)
enforces the relationship between the contract, the `java-record` selection, and
the complete
[`SimpleTransactionType.java` approved output](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/generator/java/simple-transaction-record.approved.java).

The Java profile defaults to `java-class`, so record generation requires the
explicit `java-record` model type. The result uses Java record semantics: it is
immutable and has no generated no-argument constructor or setters. Jakarta
Validation annotations describe input constraints, but the application remains
responsible for invoking validation.
