package dev.banking.asyncapi.generator.core.generator.input

import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.plan.GenerationPlan
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.MissingSchemaGenerationInput
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedPayloadSchemaFormat
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedSchemaGenerationInput
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema

/**
 * Checks of payload formats that several outputs share, and the check that every native payload is
 * handled by some planned output. Checks that belong to one output are made by that output's
 * renderer.
 */
internal object PayloadFormatChecks {
    /** Fails on the first Multi Format Schema, which [output] cannot read. */
    fun rejectMultiFormatSchemas(
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

    /** Fails unless the input has a native schema that [isSupported], naming the first schema it does have. */
    fun requireNativeSchema(
        output: String,
        generationInput: GenerationInput,
        supportedInput: String,
        isSupported: (MultiFormatSchema) -> Boolean,
    ) {
        if (generationInput.multiFormatSchemas.values.any(isSupported)) {
            return
        }
        val firstSchema = generationInput.multiFormatSchemas.entries.firstOrNull()
        val payloadName =
            firstSchema?.key
                ?: generationInput.schemas.keys.firstOrNull()
                ?: throw MissingSchemaGenerationInput(output = output, supportedInput = supportedInput)
        throw UnsupportedSchemaGenerationInput(
            output = output,
            payloadName = payloadName,
            inputFormat = firstSchema?.let { "schemaFormat '${it.value.schemaFormat}'" } ?: "an AsyncAPI Schema Object",
            supportedInput = supportedInput,
        )
    }

    /**
     * Fails on the first Multi Format Schema that no task of [generationPlan] handles: native Avro
     * and Protobuf need their native output or a Spring Kafka client that carries them, JSON Schema
     * needs the JSON Schema output, and other formats can only be bundled.
     */
    fun rejectUnhandledFormats(
        generationInput: GenerationInput,
        generationPlan: GenerationPlan,
    ) {
        val hasNativeAvro = generationPlan.tasks.any { task -> task is GenerationTask.NativeAvroArtifacts }
        val hasNativeProtobuf = generationPlan.tasks.any { task -> task is GenerationTask.NativeProtobufArtifacts }
        val hasJsonSchema = generationPlan.tasks.any { task -> task is GenerationTask.JsonSchemaArtifacts }
        val springKafkaHandledNativeSchemaNames = springKafkaHandledNativeSchemaNames(generationInput, generationPlan)
        val firstUnhandled =
            generationInput.multiFormatSchemas.entries.firstOrNull { (schemaName, schema) ->
                when {
                    schema.format.isNativeAvro ->
                        hasNativeAvro || schemaName in springKafkaHandledNativeSchemaNames
                    schema.format.isNativeProtobuf ->
                        hasNativeProtobuf || schemaName in springKafkaHandledNativeSchemaNames
                    schema.format.isJsonSchemaDraft07 -> hasJsonSchema
                    else -> false
                }.not()
            } ?: return

        throw UnsupportedPayloadSchemaFormat(
            output = "Generation",
            payloadName = firstUnhandled.key,
            schemaFormat = firstUnhandled.value.schemaFormat,
        )
    }

    private fun springKafkaHandledNativeSchemaNames(
        generationInput: GenerationInput,
        generationPlan: GenerationPlan,
    ): Set<String> {
        if (generationPlan.tasks.none { it is GenerationTask.SpringKafkaClient }) return emptySet()

        return generationInput.channels
            .asSequence()
            .flatMap { channel -> channel.messages.asSequence() }
            .mapNotNull(AnalyzedMessage::nativePayload)
            .filter { payload -> payload.schema.format.isNativeAvro || payload.schema.format.isNativeProtobuf }
            .map { payload -> payload.typeName }
            .toSet()
    }
}
