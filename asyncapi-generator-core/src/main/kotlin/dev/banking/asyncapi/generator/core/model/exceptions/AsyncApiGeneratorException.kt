package dev.banking.asyncapi.generator.core.model.exceptions

sealed class AsyncApiGeneratorException(
    message: String,
) : Exception(message) {
    class EmptyLanguageList : AsyncApiGeneratorException("The language list cannot be empty")

    class NullComponents : AsyncApiGeneratorException("The Components object cannot be null")

    class UnsupportedLanguage(
        language: String,
    ) : AsyncApiGeneratorException("The language $language is not supported")

    class InvalidEnum(
        schemaName: String,
        literal: String,
        packageName: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Enum generation failed for schema '$schemaName'. Invalid enum literal: '$literal'")
                appendLine("Enum constants must match [A-Z_][A-Z0-9_]*. Target output: $packageName.$schemaName.kt")
                appendLine()
            }.trimEnd(),
        )

    class EnumLiteralCollision(
        schemaName: String,
        originals: List<String>,
        normalized: String,
        packageName: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Enum generation failed for schema '$schemaName'. Target output: $packageName.$schemaName.kt")
                appendLine("Enum literals collide after normalization: ${formatOriginals(originals)} -> '$normalized'")
                appendLine()
            }.trimEnd(),
        ) {
        companion object {
            private fun formatOriginals(values: List<String>): String = values.joinToString(prefix = "[", postfix = "]") { "'$it'" }
        }
    }

    class InvalidKafkaHeaderName(
        headerContractName: String,
        wireName: String,
        reason: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Kafka header generation failed for '$headerContractName'.")
                appendLine("Header '$wireName' cannot be represented as a Java or Kotlin parameter.")
                appendLine("Reason: $reason")
                appendLine()
            }.trimEnd(),
        )

    class KafkaHeaderParameterNameCollision(
        headerContractName: String,
        wireNames: List<String>,
        parameterName: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Kafka header generation failed for '$headerContractName'.")
                appendLine(
                    "Header names collide after source-parameter normalization: " +
                        "${wireNames.joinToString(prefix = "[", postfix = "]") { "'$it'" }} -> '$parameterName'",
                )
                appendLine("Use distinct header names that remain unique after non-identifier characters are replaced with underscores.")
                appendLine()
            }.trimEnd(),
        )

    class UnsupportedKafkaKeySchema(
        messageName: String,
        schemaType: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Spring Kafka client generation failed for message '$messageName'.")
                appendLine("The Kafka record key uses unsupported schema type '$schemaType'.")
                appendLine("Generated Kafka key parameters currently support scalar string, integer, number, and boolean schemas.")
                appendLine("Use a scalar bindings.kafka.key schema, or omit it when the message has no contract-defined Kafka key.")
                appendLine()
            }.trimEnd(),
        )

    class UnsupportedPayloadSchemaFormat(
        output: String,
        payloadName: String,
        schemaFormat: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("$output cannot consume payload '$payloadName' because it uses schemaFormat '$schemaFormat'.")
                appendLine("This output currently supports AsyncAPI Schema Object payloads only.")
                appendLine("Native Avro, Protobuf, and other explicit schema formats must be handled by dedicated generator capabilities.")
                appendLine()
            }.trimEnd(),
        )

    class InvalidNativeAvroSchema(
        payloadName: String,
        schemaFormat: String,
        reason: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Native Avro generation failed for payload '$payloadName'.")
                appendLine("The payload uses schemaFormat '$schemaFormat', but its schema is not valid Avro.")
                appendLine("Reason: $reason")
                appendLine()
            }.trimEnd(),
        )

    class NativeAvroSpecificRecordGenerationFailed(
        payloadName: String,
        schemaFormat: String,
        reason: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("SpecificRecord generation failed for native Avro payload '$payloadName'.")
                appendLine("The payload uses schemaFormat '$schemaFormat'.")
                appendLine("Reason: $reason")
                appendLine()
            }.trimEnd(),
        )

    class UnsupportedNativeAvroPayloadType(
        payloadName: String,
        schemaFormat: String,
        schemaType: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Native Avro payload '$payloadName' cannot be used as a generated client type.")
                appendLine("The payload uses schemaFormat '$schemaFormat' with Avro schema type '$schemaType'.")
                appendLine("Generated client APIs currently require a named Avro record, enum, or fixed schema.")
                appendLine()
            }.trimEnd(),
        )

    class InvalidNativeProtobufSchema(
        payloadName: String,
        schemaFormat: String,
        reason: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Native Protobuf generation failed for payload '$payloadName'.")
                appendLine("The payload uses schemaFormat '$schemaFormat', but its schema is not valid Protobuf artifact content.")
                appendLine("Reason: $reason")
                appendLine()
            }.trimEnd(),
        )

    class NativeProtobufModelGenerationFailed(
        payloadName: String,
        schemaFormat: String,
        modelType: String,
        reason: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Protobuf $modelType model generation failed for native Protobuf payload '$payloadName'.")
                appendLine("The payload uses schemaFormat '$schemaFormat'.")
                appendLine("Reason: $reason")
                appendLine()
            }.trimEnd(),
        )

    class UnsupportedNativeProtobufPayloadType(
        payloadName: String,
        schemaFormat: String,
        reason: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Native Protobuf payload '$payloadName' cannot be used as a generated client type.")
                appendLine("The payload uses schemaFormat '$schemaFormat'.")
                appendLine("Reason: $reason")
                appendLine()
            }.trimEnd(),
        )
}
