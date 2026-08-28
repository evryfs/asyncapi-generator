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

## Spring Kafka contracts with typed headers

The
[`single-message.yaml`](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/generator/spring-kafka/single-message.yaml)
contract defines one Kafka channel and one message. The message references an
object payload, an
[`MyAccountKey` object schema](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/generator/spring-kafka/key-schemas.yaml),
and required and optional string headers.

| Configuration value                                   | Example setting                                       |
|-------------------------------------------------------|-------------------------------------------------------|
| `inputSpec`                                           | `generator/spring-kafka/single-message.yaml`          |
| `generatorName`                                       | `kotlin`                                              |
| `modelPackage`                                        | `com.example.account.model`                           |
| `clientPackage`                                       | `com.example.account.client`                          |
| `clientConfig.clientType`                             | `spring-kafka`                                        |
| `clientConfig.clientContract`                         | `interface`                                           |
| `clientConfig.producer.enabled`                       | `true` (the default)                                  |
| `clientConfig.consumer.enabled`                       | `true` (the default)                                  |
| `clientConfig.topicParameterProperties.environment`   | `kafka.environment`                                   |
| `clientConfig.validationAnnotations.clientContract`   | `org.springframework.validation.annotation.Validated` |
| `clientConfig.validationAnnotations.payloadParameter` | `jakarta.validation.Valid`                            |

The client artifacts are:

- `com/example/account/client/producer/MyAccountUpdatedProducer.kt`: complete
  [`approved producer output`](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/generator/spring-kafka/kotlin/single-message-producer.approved.kt);
- `com/example/account/client/consumer/MyAccountUpdatedConsumer.kt`: complete
  [`approved consumer output`](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/generator/spring-kafka/kotlin/single-message-consumer.approved.kt).

The `approves_kotlin_single_message_producer_and_consumer_contracts` case in
[`SpringKafkaClientApprovalTest`](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/kotlin/dev/banking/asyncapi/generator/core/generator/kafka/spring/SpringKafkaClientApprovalTest.kt)
generates both artifacts from the linked contract and protects their complete
source.

Both contracts use `MyAccountUpdatedPayload` for the message payload and the
generated
[`MyAccountKey` model](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/generator/kotlin/spring-kafka-object-key-model.approved.kt)
for the Kafka record key. `X-EXAMPLE-CORRELATION-ID` is required and becomes a
non-null `String`; `X-EXAMPLE-SOURCE-SYSTEM` is optional and becomes a nullable
`String` with a `null` default. On the consumer, the record key and received
topic are bound through `KafkaHeaders`. The channel parameter mapping produces
the topic constant `my.accounts.${kafka.environment}.updated.v1`. The
`approves_generated_kotlin_object_key_model` case in the same approval test
protects the linked key model.

These generated interfaces describe application integration contracts; they do
not configure or run Kafka. The application implements the producer send,
implements the consumer callback with `@KafkaListener`, and supplies Spring
beans, brokers, serializers, listener containers, consumer groups, error
handling, retries, and transactions. The generated validation annotations also
require the application to activate the corresponding validation behavior.
