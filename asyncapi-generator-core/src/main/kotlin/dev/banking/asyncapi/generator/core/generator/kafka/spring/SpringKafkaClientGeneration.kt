package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.java.kafka.spring.JavaSpringKafkaGenerator
import dev.banking.asyncapi.generator.core.generator.java.kafka.spring.JavaSpringKafkaSimpleGenerator
import dev.banking.asyncapi.generator.core.generator.kotlin.kafka.spring.KotlinSpringKafkaGenerator
import dev.banking.asyncapi.generator.core.generator.kotlin.kafka.spring.KotlinSpringKafkaSimpleGenerator
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName.JAVA
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName.KOTLIN
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.generator.plan.SpringKafkaClientType
import java.io.File

/**
 * Dispatches planned Spring Kafka client generation to the current implementations.
 *
 * This is the boundary for the existing Spring Kafka client generators while
 * the long-term client surface is still being designed.
 *
 * Expected behavior is covered by:
 * - `SpringKafkaClientGenerationTest`
 */
class SpringKafkaClientGeneration {
    fun generate(
        task: GenerationTask.SpringKafkaClient,
        generationInput: GenerationInput,
        sourceOutputDirectory: File,
        resourceOutputDirectory: File,
    ) {
        when (task.language) {
            KOTLIN -> generateKotlinClient(task, generationInput, sourceOutputDirectory, resourceOutputDirectory)
            JAVA -> generateJavaClient(task, generationInput, sourceOutputDirectory, resourceOutputDirectory)
        }
    }

    private fun generateKotlinClient(
        task: GenerationTask.SpringKafkaClient,
        generationInput: GenerationInput,
        sourceOutputDirectory: File,
        resourceOutputDirectory: File,
    ) {
        if (task.clientType == SpringKafkaClientType.SIMPLE) {
            val kafkaGenerator =
                KotlinSpringKafkaSimpleGenerator(
                    outputDir = sourceOutputDirectory,
                    clientPackage = task.clientPackage,
                    modelPackage = task.modelPackage,
                )
            kafkaGenerator.generate(generationInput.channels)
        } else {
            val kafkaGenerator =
                KotlinSpringKafkaGenerator(
                    outputDir = sourceOutputDirectory,
                    clientPackage = task.clientPackage,
                    modelPackage = task.modelPackage,
                    topicPropertyPrefix = task.topicPropertyPrefix,
                    resourceOutputDir = resourceOutputDirectory,
                )
            kafkaGenerator.generate(generationInput.channels)
        }
    }

    private fun generateJavaClient(
        task: GenerationTask.SpringKafkaClient,
        generationInput: GenerationInput,
        sourceOutputDirectory: File,
        resourceOutputDirectory: File,
    ) {
        if (task.clientType == SpringKafkaClientType.SIMPLE) {
            val kafkaGenerator =
                JavaSpringKafkaSimpleGenerator(
                    outputDir = sourceOutputDirectory,
                    clientPackage = task.clientPackage,
                    modelPackage = task.modelPackage,
                )
            kafkaGenerator.generate(generationInput.channels)
        } else {
            val kafkaGenerator =
                JavaSpringKafkaGenerator(
                    outputDir = sourceOutputDirectory,
                    clientPackage = task.clientPackage,
                    modelPackage = task.modelPackage,
                    topicPropertyPrefix = task.topicPropertyPrefix,
                    resourceOutputDir = resourceOutputDirectory,
                )
            kafkaGenerator.generate(generationInput.channels)
        }
    }
}
