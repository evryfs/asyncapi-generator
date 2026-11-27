package dev.banking.asyncapi.generator.core.model.exceptions

sealed class AsyncApiGeneratorException(
    message: String,
) : Exception(message) {
    /**
     * Two artifacts of one run would be written to the same file, so one of them would be lost.
     *
     * @param destination the file both would be written to
     * @param artifacts the colliding artifacts, by kind and path
     */
    class GeneratedArtifactCollision(
        destination: String,
        artifacts: List<String>,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Generated artifacts resolve to the same output file: $destination")
                artifacts.forEach { artifact -> appendLine("- $artifact") }
                appendLine("Configure distinct output files, packages, or output directories for these artifacts.")
                appendLine()
            }.trimEnd(),
        )

    /** Two different inline schemas would be generated under one name, so one of them would be lost. */
    class InlineSchemaNameCollision(
        val schemaName: String,
        val location: String,
        val existingLocation: String?,
    ) : AsyncApiGeneratorException(
            "Inline schema at $location would be generated as '$schemaName', which is already used by the " +
                "different schema at $existingLocation. Give one of them a distinct title, or define it once " +
                "under components.schemas and reference it.",
        )

    /**
     * A model named like a type that the generated sources use without qualification, which would
     * shadow that type or be shadowed by its import.
     */
    class ReservedModelName(
        val modelName: String,
        val language: String,
    ) : AsyncApiGeneratorException(
            "Model '$modelName' has the name of a type that generated $language code already uses. " +
                "Rename the schema it is generated from in the contract.",
        )

    /** A name Avro rejects; renaming it silently would change a schema that may already be registered. */
    class PropertyNameCollision(
        val schemaName: String,
        val wireNames: List<String>,
        val identifier: String,
    ) : AsyncApiGeneratorException(
            "Schema '$schemaName' has properties ${wireNames.joinToString(prefix = "[", postfix = "]") { "'$it'" }} " +
                "that all become the source identifier '$identifier'. Rename one of them in the contract.",
        )

    /** A generated `.avsc` that Apache Avro does not accept, which would fail only when a consumer loads it. */
    class InvalidGeneratedAvroSchema(
        val schemaPath: String,
        reason: String,
    ) : AsyncApiGeneratorException("Generated Avro schema '$schemaPath' is not valid: $reason")

    class InvalidAvroName(
        val kind: String,
        val avroName: String,
        val location: String,
    ) : AsyncApiGeneratorException(
            "Avro schema generation failed: $kind '$avroName' in $location is not a valid Avro name. " +
                "Avro names must match [A-Za-z_][A-Za-z0-9_]*. Rename it in the contract.",
        )

    /**
     * Avro Projection meets a value that may be anything, such as an open object, which no Avro type describes.
     *
     * @param location the property or schema that holds the value
     */
    class UntypedAvroValue(
        val location: String,
    ) : AsyncApiGeneratorException(
            "Avro schema generation failed: $location accepts any value, and Avro has no type for that. " +
                "Give it a type in the contract.",
        )

    class NoArtifactsGenerated :
        AsyncApiGeneratorException(
            "Generation completed without producing any artifacts.",
        )

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
                appendLine(
                    "Enum generation failed for schema '$schemaName'. Target output: $packageName.$schemaName.kt",
                )
                appendLine("Enum literals collide after normalization: ${formatOriginals(originals)} -> '$normalized'")
                appendLine()
            }.trimEnd(),
        ) {
        companion object {
            private fun formatOriginals(values: List<String>): String =
                values.joinToString(prefix = "[", postfix = "]") { "'$it'" }
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
        fixedParameter: Boolean = false,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Kafka header generation failed for '$headerContractName'.")
                val names = wireNames.joinToString(prefix = "[", postfix = "]") { "'$it'" }
                if (fixedParameter) {
                    appendLine(
                        "Header name $names becomes the parameter '$parameterName', which the generated " +
                            "method already declares for the payload, topic, or key.",
                    )
                    appendLine(
                        "Rename the header so that it differs from payload, receivedTopic, receivedKey, and messageKey.",
                    )
                } else {
                    appendLine("Header names collide after source-parameter normalization: $names -> '$parameterName'")
                    appendLine(
                        "Use distinct header names that remain unique after non-identifier characters are replaced with underscores.",
                    )
                }
                appendLine()
            }.trimEnd(),
        )

    class UnsupportedKafkaHeaderSchema(
        headerContractName: String,
        wireName: String,
        schemaType: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Spring Kafka client generation failed for header contract '$headerContractName'.")
                appendLine("Header '$wireName' uses unsupported schema type '$schemaType'.")
                appendLine(
                    "Generated Kafka header parameters support scalar string, integer, number, and boolean schemas.",
                )
                appendLine("Use a supported scalar schema for this message header.")
                appendLine()
            }.trimEnd(),
        )

    class SpringKafkaClientMethodNameCollision(
        channelName: String,
        messageIds: List<String>,
        methodName: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Spring Kafka client generation failed for channel '$channelName'.")
                appendLine(
                    "Channel messages " +
                        messageIds.joinToString(prefix = "[", postfix = "]") { "'$it'" } +
                        " resolve to generated client method '$methodName'.",
                )
                appendLine("This method would be generated more than once in the same client contract.")
                appendLine(
                    "Give each Message Object a unique 'name', or use unique channel message keys when 'name' is omitted.",
                )
                appendLine(
                    "Names must remain unique after conversion to source-code identifiers, " +
                        "including any configured producer method suffixes.",
                )
                appendLine()
            }.trimEnd(),
        )

    class SpringKafkaClientChannelWithoutMessages(
        channelName: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Spring Kafka client generation failed for channel '$channelName'.")
                appendLine("The channel does not declare any messages.")
                appendLine(
                    "Declare at least one Message Object under channels.$channelName.messages " +
                        "before generating client contracts.",
                )
                appendLine()
            }.trimEnd(),
        )

    class SpringKafkaClientChannelWithoutAddress(
        channelName: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Spring Kafka client generation failed for channel '$channelName'.")
                appendLine("The AsyncAPI channel has no address.")
                appendLine("Declare channels.$channelName.address before generating client contracts.")
                appendLine()
            }.trimEnd(),
        )

    class SpringKafkaClientContractNameCollision(
        channelNames: List<String>,
        generatedBaseName: String,
        contractNames: List<String>,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Spring Kafka client generation failed because channel IDs collide after normalization.")
                appendLine(
                    "${channelNames.joinToString(prefix = "[", postfix = "]") { "'$it'" }} " +
                        "resolve to generated contract base name '$generatedBaseName'.",
                )
                appendLine(
                    "The following contracts would be written more than once: " +
                        contractNames.joinToString(prefix = "[", postfix = "]") { "'$it'" },
                )
                appendLine(
                    "Use channel IDs that remain unique after conversion to PascalCase source-code identifiers.",
                )
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
                appendLine(
                    "Generated Kafka key parameters support scalar string, integer, number, and boolean schemas, " +
                        "and object schemas represented by generated key models.",
                )
                appendLine(
                    "Use a supported bindings.kafka.key schema, or omit it when the message has no contract-defined Kafka key.",
                )
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
                appendLine(
                    "$output cannot consume payload '$payloadName' because it uses schemaFormat '$schemaFormat'.",
                )
                appendLine("This output currently supports AsyncAPI Schema Object payloads only.")
                appendLine(
                    "Native Avro, Protobuf, and other explicit schema formats must be handled by dedicated generator capabilities.",
                )
                appendLine()
            }.trimEnd(),
        )

    class UnsupportedSchemaGenerationInput(
        output: String,
        payloadName: String,
        inputFormat: String,
        supportedInput: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("$output cannot consume payload '$payloadName' because it uses $inputFormat.")
                appendLine("Supported input: $supportedInput.")
                appendLine()
            }.trimEnd(),
        )

    internal class UnsupportedSourceSchemaFeature(
        output: String,
        rootSchemaName: String,
        schemaPath: String,
        feature: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("$output cannot represent schema '$rootSchemaName'.")
                appendLine("Incompatible schema path: $schemaPath")
                appendLine("Incompatible feature: $feature.")
                appendLine(
                    "Use a compatible schema shape for this output, or remove the output from generator configuration.",
                )
                appendLine()
            }.trimEnd(),
        )

    class MissingSchemaGenerationInput(
        output: String,
        supportedInput: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("$output did not find any compatible schemas in the AsyncAPI document.")
                appendLine("Supported input: $supportedInput.")
                appendLine()
            }.trimEnd(),
        )

    class InvalidJsonSchema(
        payloadName: String,
        reason: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("JSON Schema generation failed for payload '$payloadName'.")
                appendLine("Reason: $reason")
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

    class NativeAvroModelPackageMismatch(
        payloadName: String,
        configuredPackage: String,
        schemaNamespace: String?,
    ) : AsyncApiGeneratorException(
            buildString {
                val namespace = schemaNamespace ?: "<default package>"
                appendLine()
                appendLine("Native Avro model generation failed for payload '$payloadName'.")
                appendLine(
                    "Configured modelPackage '$configuredPackage' does not match " +
                        "the Avro namespace '$namespace'.",
                )
                appendLine(
                    "Apache Avro determines SpecificRecord packages from the schema namespace; " +
                        "the generator cannot override it safely.",
                )
                if (schemaNamespace == null) {
                    appendLine(
                        "Add namespace '$configuredPackage' to the native Avro schema.",
                    )
                } else {
                    appendLine(
                        "Set modelPackage to '$schemaNamespace' or update the native Avro schema namespace.",
                    )
                }
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
                appendLine(
                    "The payload uses schemaFormat '$schemaFormat', but its schema is not valid Protobuf artifact content.",
                )
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

    class SchemaNameCollision(
        generatedName: String,
        firstOrigin: String,
        conflictingOrigin: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Distinct schema declarations resolve to generated name '$generatedName'.")
                appendLine("First declaration: $firstOrigin")
                appendLine("Conflicting declaration: $conflictingOrigin")
                appendLine("Rename one declaration, or share a single declaration through a reference.")
                appendLine()
            }.trimEnd(),
        )
}
