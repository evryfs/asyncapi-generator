package dev.banking.asyncapi.generator.core.generator.input

import dev.banking.asyncapi.generator.core.generator.plan.GenerationPlan
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
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
        generationPlan.tasks.forEach { task ->
            when (task) {
                is GenerationTask.ModelArtifacts ->
                    rejectMultiFormatSchemas(
                        output = "Model generation",
                        multiFormatSchemas = generationInput.multiFormatSchemas,
                    )
                is GenerationTask.SpringKafkaClient ->
                    rejectMultiFormatMessages(
                        output = "Spring Kafka client generation",
                        generationInput = generationInput,
                    )
                is GenerationTask.AvroSchemaArtifacts ->
                    rejectMultiFormatSchemas(
                        output = "Avro Projection",
                        multiFormatSchemas = generationInput.multiFormatSchemas,
                    )
                is GenerationTask.HeaderModelArtifacts,
                is GenerationTask.QuarkusKafkaClient,
                -> Unit
            }
        }
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

    private fun rejectMultiFormatMessages(
        output: String,
        generationInput: GenerationInput,
    ) {
        val firstMessage =
            generationInput.channels
                .asSequence()
                .flatMap { channel -> channel.multiFormatMessages.asSequence() }
                .firstOrNull() ?: return

        throw UnsupportedPayloadSchemaFormat(
            output = output,
            payloadName = firstMessage.payloadName,
            schemaFormat = firstMessage.schema.schemaFormat,
        )
    }
}
