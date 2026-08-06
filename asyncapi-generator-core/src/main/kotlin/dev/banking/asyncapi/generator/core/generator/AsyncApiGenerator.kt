package dev.banking.asyncapi.generator.core.generator

import dev.banking.asyncapi.generator.core.generator.artifact.AvroSchemaArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.artifact.DocumentArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.artifact.JsonSchemaArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.artifact.ModelArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.artifact.NativeAvroArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.artifact.NativeProtobufArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.input.GenerationInputCompatibilityValidator
import dev.banking.asyncapi.generator.core.generator.input.GenerationInputFactory
import dev.banking.asyncapi.generator.core.generator.kafka.spring.SpringKafkaClientGeneration
import dev.banking.asyncapi.generator.core.generator.output.FileSystemGeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.plan.GenerationPlanner
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedGenerationCapability

/**
 * Coordinates generator input preparation, planning, rendering, and artifact writing.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorOutputContractTest`
 */
class AsyncApiGenerator {
    private val generationInputFactory = GenerationInputFactory()
    private val generationInputCompatibilityValidator = GenerationInputCompatibilityValidator()
    private val generationPlanner = GenerationPlanner()
    private val documentArtifactGeneration = DocumentArtifactGeneration()
    private val modelArtifactGeneration = ModelArtifactGeneration()
    private val avroSchemaArtifactGeneration = AvroSchemaArtifactGeneration()
    private val jsonSchemaArtifactGeneration = JsonSchemaArtifactGeneration()
    private val nativeAvroArtifactGeneration = NativeAvroArtifactGeneration()
    private val nativeProtobufArtifactGeneration = NativeProtobufArtifactGeneration()
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
                javaSourceOutputDirectory = generatorConfiguration.output.javaSourceOutputDirectory,
            )

        generationPlan.tasks.forEach { task ->
            when (task) {
                is GenerationTask.DocumentArtifact ->
                    documentArtifactGeneration.generate(
                        task = task,
                        asyncApiDocument = asyncApiDocument,
                    )
                is GenerationTask.ModelArtifacts ->
                    modelArtifactGeneration.generateModelArtifacts(
                        task = task,
                        generationInput = generationInput,
                        artifactWriter = artifactWriter,
                    )
                is GenerationTask.KafkaKeyModelArtifacts ->
                    modelArtifactGeneration.generateKafkaKeyModelArtifacts(
                        task = task,
                        generationInput = generationInput,
                        artifactWriter = artifactWriter,
                    )
                is GenerationTask.SpringKafkaClient ->
                    artifactWriter.write(
                        springKafkaClientGeneration.render(
                            task = task,
                            generationInput = generationInput,
                        ),
                    )
                is GenerationTask.QuarkusKafkaClient ->
                    throw UnsupportedGenerationCapability("Quarkus Kafka client generation")
                is GenerationTask.NativeAvroArtifacts ->
                    nativeAvroArtifactGeneration.generate(
                        task = task,
                        generationInput = generationInput,
                        artifactWriter = artifactWriter,
                    )
                is GenerationTask.NativeProtobufArtifacts ->
                    nativeProtobufArtifactGeneration.generate(
                        task = task,
                        generationInput = generationInput,
                        artifactWriter = artifactWriter,
                    )
                is GenerationTask.AvroSchemaArtifacts ->
                    avroSchemaArtifactGeneration.generate(
                        task = task,
                        generationInput = generationInput,
                        artifactWriter = artifactWriter,
                    )
                is GenerationTask.JsonSchemaArtifacts ->
                    jsonSchemaArtifactGeneration.generate(
                        task = task,
                        generationInput = generationInput,
                        artifactWriter = artifactWriter,
                    )
            }
        }
    }
}
