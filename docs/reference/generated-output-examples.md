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

## Native Avro schema and SpecificRecord

The
[`asyncapi_native_avro_spring_kafka_client.yaml`](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/generator/asyncapi_native_avro_spring_kafka_client.yaml)
contract uses a Multi Format Schema Object to embed a native Avro `UserCreated`
record.

| Configuration value     | Example setting                                          |
|-------------------------|----------------------------------------------------------|
| `inputSpec`             | `asyncapi_native_avro_spring_kafka_client.yaml`          |
| `generatorName`         | `java`                                                   |
| `modelPackage`          | `com.example.avro`                                       |
| `modelConfig.modelType` | `avro-specific-record`                                   |
| `schemaPackage`         | `com.example.avro`                                       |

The generated artifacts are:

- `com/example/avro/UserCreated.avsc`: complete
  [`approved native Avro schema`](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/generator/native-avro/schema/user-created.approved.avsc);
- `com/example/avro/UserCreated.java`: complete
  [`approved SpecificRecord source`](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/generator/native-avro/specific-record/user-created.approved.java).

[`NativeAvroApprovalTest`](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/kotlin/dev/banking/asyncapi/generator/core/generator/avro/NativeAvroApprovalTest.kt)
parses and resolves the linked contract before approving both complete artifacts.

SpecificRecord generation requires a native Avro schema with a `namespace`, and
`modelPackage` must exactly match that namespace. The runtime model is generated
Java source even when the Kotlin source profile is selected. The application
must provide the Avro runtime used by the generated class.

## Avro Projection from an AsyncAPI Schema Object

The
[`asyncapi_enum_default_value.yaml`](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/generator/asyncapi_enum_default_value.yaml)
contract defines `Task`, `TaskStatus`, and an inline `Priority` enum as AsyncAPI
Schema Objects rather than native Avro schemas.

| Configuration value | Example setting                     |
|---------------------|-------------------------------------|
| `inputSpec`         | `asyncapi_enum_default_value.yaml`  |
| `generatorName`     | `avro-schema`                       |
| `schemaPackage`     | `com.example.avro`                  |

The approved artifact is `com/example/avro/Task.avsc`: inspect the complete
[`approved projected Avro schema`](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/generator/avro/task-schema.approved.avsc).
[`AvroSchemaApprovalTest`](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/kotlin/dev/banking/asyncapi/generator/core/generator/avro/AvroSchemaApprovalTest.kt)
generates that artifact from the linked contract.

Avro Projection maps supported AsyncAPI Schema Object structure into Avro; it is
not native Avro passthrough and does not generate SpecificRecord source. Use a
native Avro Multi Format Schema Object and `avro-specific-record` when the input
schema and generated Avro runtime model must follow native Avro semantics.

## Native Protobuf schema artifact

The
[`native-protobuf.yaml`](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/examples/generated-output/native-protobuf.yaml)
contract uses a Multi Format Schema Object to embed a native Protobuf
`UserCreated` message.

| Configuration value | Example setting          |
|---------------------|--------------------------|
| `inputSpec`         | `native-protobuf.yaml`   |
| `generatorName`     | `protobuf-schema`        |
| `schemaPackage`     | `com.example.protobuf`   |

The run generates `com/example/protobuf/UserCreated.proto`: inspect the complete
[`approved native Protobuf schema`](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/generator/native-protobuf/schema/user-created.approved.proto).
[`NativeProtobufApprovalTest`](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/kotlin/dev/banking/asyncapi/generator/core/generator/protobuf/NativeProtobufApprovalTest.kt)
parses and resolves the linked contract before approving the complete artifact.

`protobuf-schema` copies native Protobuf schema content without generating Java
messages or Kotlin DSL source. `schemaPackage` controls the artifact output path;
it does not rewrite the Protobuf `package`. Runtime message generation instead
uses a Java or Kotlin source profile with `modelType=protobuf-message` and has
additional package and `java_multiple_files` input requirements.
