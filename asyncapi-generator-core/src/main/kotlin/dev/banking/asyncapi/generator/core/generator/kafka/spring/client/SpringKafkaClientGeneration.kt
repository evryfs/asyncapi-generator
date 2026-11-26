package dev.banking.asyncapi.generator.core.generator.kafka.spring.client

import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.input.SourceSchemaCompatibilityValidator
import dev.banking.asyncapi.generator.core.generator.kafka.spring.SpringKafkaClientContractValidator
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.generator.task.GenerationContext
import dev.banking.asyncapi.generator.core.generator.task.TaskRenderer
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedPayloadSchemaFormat

/**
 * Renders a planned Spring Kafka client with the client syntax of its language. Native payloads
 * must be Avro or Protobuf, the only formats a client can carry.
 *
 * @param syntaxOf the Spring Kafka client syntax of a language
 */
internal class SpringKafkaClientGeneration(
    private val syntaxOf: (SourceLanguage) -> SpringKafkaClientSyntax,
) : TaskRenderer<GenerationTask.SpringKafkaClient> {
    override fun validate(
        task: GenerationTask.SpringKafkaClient,
        context: GenerationContext,
    ) {
        val generationInput = context.input
        SpringKafkaClientContractValidator.validate(channels = generationInput.channels, task = task)
        rejectUnsupportedNativePayloads(generationInput)
        generationInput.channels.forEach { channel ->
            channel.messages.forEach { message ->
                message.kafkaKey?.let { kafkaKey ->
                    SourceSchemaCompatibilityValidator.validateRootJavaPattern(
                        output = OUTPUT,
                        rootSchemaName = message.messageName,
                        schema = kafkaKey.requireSchema(),
                    )
                }
            }
        }
    }

    override fun render(
        task: GenerationTask.SpringKafkaClient,
        context: GenerationContext,
    ): GenerationResult = render(task, context.input)

    /** The client of [task] built from the channels of [generationInput]. */
    fun render(
        task: GenerationTask.SpringKafkaClient,
        generationInput: GenerationInput,
    ): GenerationResult {
        val syntax = syntaxOf(task.language)
        val emitter =
            SpringKafkaClientEmitter(
                syntax = syntax,
                viewFactory =
                    SpringKafkaClientViewFactory(
                        syntax = syntax,
                        clientPackage = task.client.packageName,
                        modelPackage = task.client.modelPackageName,
                        generateProducers = task.client.producer.enabled,
                        additionalPayloadTypes = task.client.producer.additionalPayloadTypes,
                        generateConsumers = task.client.consumer.enabled,
                        topicParameterProperties = task.client.topicParameterProperties,
                        validationAnnotations = task.client.validationAnnotations,
                    ),
            )
        return emitter.emit(generationInput.channels)
    }

    private fun rejectUnsupportedNativePayloads(generationInput: GenerationInput) {
        val firstPayload =
            generationInput.channels
                .asSequence()
                .flatMap { channel -> channel.messages.asSequence() }
                .mapNotNull { message -> message.nativePayload }
                .filterNot { payload -> payload.schema.format.isNativeAvro || payload.schema.format.isNativeProtobuf }
                .firstOrNull() ?: return

        throw UnsupportedPayloadSchemaFormat(
            output = OUTPUT,
            payloadName = firstPayload.typeName,
            schemaFormat = firstPayload.schema.schemaFormat,
        )
    }

    private companion object {
        const val OUTPUT = "Spring Kafka client generation"
    }
}
