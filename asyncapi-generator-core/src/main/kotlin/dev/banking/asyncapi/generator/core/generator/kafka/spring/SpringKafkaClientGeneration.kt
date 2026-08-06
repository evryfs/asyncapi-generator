package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.java.kafka.spring.JavaSpringKafkaGenerator
import dev.banking.asyncapi.generator.core.generator.kotlin.kafka.spring.KotlinSpringKafkaGenerator
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage.JAVA
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage.KOTLIN
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask

/**
 * Dispatches planned Spring Kafka client generation to the supported contract generator.
 *
 * Expected behavior is covered by:
 * - `SpringKafkaClientGenerationTest`
 */
class SpringKafkaClientGeneration {
    fun render(
        task: GenerationTask.SpringKafkaClient,
        generationInput: GenerationInput,
    ): GenerationResult =
        when (task.language) {
            KOTLIN -> renderKotlinClient(task, generationInput)
            JAVA -> renderJavaClient(task, generationInput)
        }

    private fun renderKotlinClient(
        task: GenerationTask.SpringKafkaClient,
        generationInput: GenerationInput,
    ): GenerationResult {
        val kafkaGenerator =
            KotlinSpringKafkaGenerator(
                clientPackage = task.clientPackage,
                modelPackage = task.modelPackage,
                generateProducers = task.generateProducers,
                generateConsumers = task.generateConsumers,
                topicParameterProperties = task.topicParameterProperties,
                validationAnnotations = task.validationAnnotations,
            )
        return kafkaGenerator.render(generationInput.channels)
    }

    private fun renderJavaClient(
        task: GenerationTask.SpringKafkaClient,
        generationInput: GenerationInput,
    ): GenerationResult {
        val kafkaGenerator =
            JavaSpringKafkaGenerator(
                clientPackage = task.clientPackage,
                modelPackage = task.modelPackage,
                generateProducers = task.generateProducers,
                generateConsumers = task.generateConsumers,
                topicParameterProperties = task.topicParameterProperties,
                validationAnnotations = task.validationAnnotations,
            )
        return kafkaGenerator.render(generationInput.channels)
    }
}
