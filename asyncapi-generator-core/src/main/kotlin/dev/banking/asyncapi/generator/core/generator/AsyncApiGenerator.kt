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
                    artifactWriter.write(
                        modelArtifactGeneration.renderModelArtifacts(
                            task = task,
                            generationInput = generationInput,
                        ),
                    )
                is GenerationTask.KafkaKeyModelArtifacts ->
                    artifactWriter.write(
                        modelArtifactGeneration.renderKafkaKeyModelArtifacts(
                            task = task,
                            generationInput = generationInput,
                        ),
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
                    artifactWriter.write(
                        nativeAvroArtifactGeneration.render(
                            task = task,
                            generationInput = generationInput,
                        ),
                    )
                is GenerationTask.NativeProtobufArtifacts ->
                    artifactWriter.write(
                        nativeProtobufArtifactGeneration.render(
                            task = task,
                            generationInput = generationInput,
                        ),
                    )
                is GenerationTask.AvroSchemaArtifacts ->
                    artifactWriter.write(
                        avroSchemaArtifactGeneration.render(
                            task = task,
                            generationInput = generationInput,
                        ),
                    )
                is GenerationTask.JsonSchemaArtifacts ->
                    artifactWriter.write(
                        jsonSchemaArtifactGeneration.render(
                            task = task,
                            generationInput = generationInput,
                        ),
                    )
            }
        }
    }
}
