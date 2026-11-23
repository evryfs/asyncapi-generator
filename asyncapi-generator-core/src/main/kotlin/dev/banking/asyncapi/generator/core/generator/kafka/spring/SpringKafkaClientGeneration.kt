package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage.JAVA
import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage.KOTLIN
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.ir.ClientEmitter
import dev.banking.asyncapi.generator.core.generator.java.kafka.spring.JavaSpringKafkaGenerator
import dev.banking.asyncapi.generator.core.generator.kotlin.kafka.spring.KotlinSpringKafkaGenerator
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask

/** Dispatches planned Spring Kafka client generation to the client emitter of its language. */
internal class SpringKafkaClientGeneration {
    fun render(
        task: GenerationTask.SpringKafkaClient,
        generationInput: GenerationInput,
    ): GenerationResult = emitterFor(task).emit(generationInput.channels)

    private fun emitterFor(task: GenerationTask.SpringKafkaClient): ClientEmitter =
        when (task.language) {
            KOTLIN ->
                KotlinSpringKafkaGenerator(
                    clientPackage = task.client.packageName,
                    modelPackage = task.client.modelPackageName,
                    generateProducers = task.client.producer.enabled,
                    additionalPayloadTypes = task.client.producer.additionalPayloadTypes,
                    generateConsumers = task.client.consumer.enabled,
                    topicParameterProperties = task.client.topicParameterProperties,
                    validationAnnotations = task.client.validationAnnotations,
                )
            JAVA ->
                JavaSpringKafkaGenerator(
                    clientPackage = task.client.packageName,
                    modelPackage = task.client.modelPackageName,
                    generateProducers = task.client.producer.enabled,
                    additionalPayloadTypes = task.client.producer.additionalPayloadTypes,
                    generateConsumers = task.client.consumer.enabled,
                    topicParameterProperties = task.client.topicParameterProperties,
                    validationAnnotations = task.client.validationAnnotations,
                )
        }
}
