package dev.banking.asyncapi.generator.core.generator.kafka.spring.client

import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask

/**
 * Renders a planned Spring Kafka client with the client syntax of its language.
 *
 * @param syntaxOf the Spring Kafka client syntax of a language
 */
internal class SpringKafkaClientGeneration(
    private val syntaxOf: (SourceLanguage) -> SpringKafkaClientSyntax,
) {
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
}
