package dev.banking.asyncapi.generator.core.generator

import dev.banking.asyncapi.generator.core.generator.artifact.AvroSchemaArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.artifact.ModelArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.input.GenerationInputCompatibilityValidator
import dev.banking.asyncapi.generator.core.generator.input.GenerationInputFactory
import dev.banking.asyncapi.generator.core.generator.kafka.spring.SpringKafkaClientGeneration
import dev.banking.asyncapi.generator.core.generator.output.FileSystemGeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.plan.GenerationPlanner
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import org.slf4j.LoggerFactory

/**
 * Coordinates generator input preparation, planning, rendering, and artifact writing.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorOutputContractTest`
 */
class AsyncApiGenerator {
    private val log = LoggerFactory.getLogger(AsyncApiGenerator::class.java)

    private val generationInputFactory = GenerationInputFactory()
    private val generationInputCompatibilityValidator = GenerationInputCompatibilityValidator()
    private val generationPlanner = GenerationPlanner()
    private val modelArtifactGeneration = ModelArtifactGeneration()
    private val avroSchemaArtifactGeneration = AvroSchemaArtifactGeneration()
    private val springKafkaClientGeneration = SpringKafkaClientGeneration()

    fun generate(
        asyncApiDocument: AsyncApiDocument,
        generatorConfiguration: GeneratorConfiguration,
    ) {
        val generationInput = generationInputFactory.create(asyncApiDocument)
        val generationPlan = generationPlanner.plan(generatorConfiguration)
        generationInputCompatibilityValidator.validate(
            generationInput = generationInput,
            generationPlan = generationPlan,
        )
        val artifactWriter =
            FileSystemGeneratedArtifactWriter(
                sourceOutputDirectory = generatorConfiguration.output.sourceOutputDirectory,
                resourceOutputDirectory = generatorConfiguration.output.resourceOutputDirectory,
            )

        generationPlan.tasks.forEach { task ->
            when (task) {
                is GenerationTask.ModelArtifacts ->
                    modelArtifactGeneration.generateModelArtifacts(
                        task = task,
                        generationInput = generationInput,
                        sourceOutputDirectory = generatorConfiguration.output.sourceOutputDirectory,
                        artifactWriter = artifactWriter,
                    )
                is GenerationTask.HeaderModelArtifacts ->
                    modelArtifactGeneration.generateHeaderModelArtifacts(
                        task = task,
                        asyncApiDocument = asyncApiDocument,
                        generationInput = generationInput,
                        sourceOutputDirectory = generatorConfiguration.output.sourceOutputDirectory,
                        artifactWriter = artifactWriter,
                    )
                is GenerationTask.SpringKafkaClient ->
                    springKafkaClientGeneration.generate(
                        task = task,
                        generationInput = generationInput,
                        sourceOutputDirectory = generatorConfiguration.output.sourceOutputDirectory,
                        resourceOutputDirectory = generatorConfiguration.output.resourceOutputDirectory,
                    )
                is GenerationTask.QuarkusKafkaClient ->
                    log.info("Generate ${task.language.name.titlecase()} Quarkus Kafka Client is not yet implemented. Skipping..")
                is GenerationTask.AvroSchemaArtifacts ->
                    avroSchemaArtifactGeneration.generate(
                        task = task,
                        generationInput = generationInput,
                        resourceOutputDirectory = generatorConfiguration.output.resourceOutputDirectory,
                        artifactWriter = artifactWriter,
                    )
            }
        }
    }

    private fun String.titlecase(): String =
        lowercase().replaceFirstChar { it.titlecase() }
}
