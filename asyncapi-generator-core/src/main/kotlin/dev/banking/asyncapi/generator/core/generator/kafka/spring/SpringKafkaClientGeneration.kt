package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.java.kafka.spring.JavaSpringKafkaGenerator
import dev.banking.asyncapi.generator.core.generator.kotlin.kafka.spring.KotlinSpringKafkaGenerator
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName.JAVA
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName.KOTLIN
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import java.io.File

/**
 * Dispatches planned Spring Kafka client generation to the supported contract generator.
 *
 * Expected behavior is covered by:
 * - `SpringKafkaClientGenerationTest`
 */
class SpringKafkaClientGeneration {
    fun generate(
        task: GenerationTask.SpringKafkaClient,
        generationInput: GenerationInput,
        sourceOutputDirectory: File,
        @Suppress("UNUSED_PARAMETER")
        resourceOutputDirectory: File,
    ) {
        when (task.language) {
            KOTLIN -> generateKotlinClient(task, generationInput, sourceOutputDirectory)
            JAVA -> generateJavaClient(task, generationInput, sourceOutputDirectory)
        }
    }

    private fun generateKotlinClient(
        task: GenerationTask.SpringKafkaClient,
        generationInput: GenerationInput,
        sourceOutputDirectory: File,
    ) {
        val kafkaGenerator =
            KotlinSpringKafkaGenerator(
                outputDir = sourceOutputDirectory,
                clientPackage = task.clientPackage,
                modelPackage = task.modelPackage,
                generateProducers = task.generateProducers,
                generateConsumers = task.generateConsumers,
            )
        kafkaGenerator.generate(generationInput.channels)
    }

    private fun generateJavaClient(
        task: GenerationTask.SpringKafkaClient,
        generationInput: GenerationInput,
        sourceOutputDirectory: File,
    ) {
        val kafkaGenerator =
            JavaSpringKafkaGenerator(
                outputDir = sourceOutputDirectory,
                clientPackage = task.clientPackage,
                modelPackage = task.modelPackage,
                generateProducers = task.generateProducers,
                generateConsumers = task.generateConsumers,
            )
        kafkaGenerator.generate(generationInput.channels)
    }
}
