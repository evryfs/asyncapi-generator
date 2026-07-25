package dev.banking.asyncapi.generator.core.generator.input

import dev.banking.asyncapi.generator.core.generator.plan.GenerationPlan
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedSchemaGenerationInput
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedPayloadSchemaFormat
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema

/**
 * Validates that planned generator outputs can consume the prepared generator input.
 *
 * Expected behavior is covered by:
 * - `GenerationInputCompatibilityValidatorTest`
 */
class GenerationInputCompatibilityValidator {

    fun validate(
        generationInput: GenerationInput,
        generationPlan: GenerationPlan,
    ) {
        val hasAvroProjection =
            generationPlan.tasks.any { task -> task is GenerationTask.AvroSchemaArtifacts }
        val hasNativeAvro =
            generationPlan.tasks.any { task -> task is GenerationTask.NativeAvroArtifacts }

        generationPlan.tasks.forEach { task ->
            when (task) {
                is GenerationTask.ModelArtifacts ->
                    rejectMultiFormatSchemas(
                        output = "Model generation",
                        multiFormatSchemas = generationInput.multiFormatSchemas,
                    )
                is GenerationTask.SpringKafkaClient ->
                    rejectUnsupportedMultiFormatMessages(
                        output = "Spring Kafka client generation",
                        generationInput = generationInput,
                    )
                is GenerationTask.AvroSchemaArtifacts ->
                    if (!hasNativeAvro) {
                        rejectMultiFormatSchemas(
                            output = "Avro Projection",
                            multiFormatSchemas = generationInput.multiFormatSchemas,
                        )
                    }
                is GenerationTask.NativeAvroArtifacts -> {
                    if (
                        task.generateSpecificRecords ||
                        !hasAvroProjection ||
                        generationInput.schemas.isEmpty()
                    ) {
                        requireNativeSchema(
                            output = "Native Avro generation",
                            generationInput = generationInput,
                            supportedInput = "native Avro schemas",
                            isSupported = { schema -> schema.format.isNativeAvro },
                        )
                    }
                }
                is GenerationTask.NativeProtobufArtifacts -> {
                    requireNativeSchema(
                        output = "Native Protobuf generation",
                        generationInput = generationInput,
                        supportedInput = "native Protobuf schemas",
                        isSupported = { schema -> schema.format.isNativeProtobuf },
                    )
                }
                is GenerationTask.HeaderModelArtifacts,
                is GenerationTask.QuarkusKafkaClient,
                -> Unit
            }
        }
    }

    private fun requireNativeSchema(
        output: String,
        generationInput: GenerationInput,
        supportedInput: String,
        isSupported: (MultiFormatSchema) -> Boolean,
    ) {
        if (generationInput.multiFormatSchemas.values.any(isSupported)) {
            return
        }

        val firstSchema =
            generationInput.multiFormatSchemas.entries.firstOrNull()
        if (firstSchema != null) {
            throw UnsupportedSchemaGenerationInput(
                output = output,
                payloadName = firstSchema.key,
                inputFormat = "schemaFormat '${firstSchema.value.schemaFormat}'",
                supportedInput = supportedInput,
            )
        }

        val payloadName = generationInput.schemas.keys.firstOrNull() ?: return
        throw UnsupportedSchemaGenerationInput(
            output = output,
            payloadName = payloadName,
            inputFormat = "an AsyncAPI Schema Object",
            supportedInput = supportedInput,
        )
    }

    private fun rejectMultiFormatSchemas(
        output: String,
        multiFormatSchemas: Map<String, MultiFormatSchema>,
    ) {
        val firstSchema = multiFormatSchemas.entries.firstOrNull() ?: return
        throw UnsupportedPayloadSchemaFormat(
            output = output,
            payloadName = firstSchema.key,
            schemaFormat = firstSchema.value.schemaFormat,
        )
    }

    private fun rejectUnsupportedMultiFormatMessages(
        output: String,
        generationInput: GenerationInput,
    ) {
        val firstMessage =
            generationInput.channels
                .asSequence()
                .flatMap { channel -> channel.multiFormatMessages.asSequence() }
                .filterNot { message -> message.schema.format.isNativeAvro || message.schema.format.isNativeProtobuf }
                .firstOrNull() ?: return

        throw UnsupportedPayloadSchemaFormat(
            output = output,
            payloadName = firstMessage.payloadName,
            schemaFormat = firstMessage.schema.schemaFormat,
        )
    }
}
