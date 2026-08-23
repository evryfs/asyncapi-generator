package dev.banking.asyncapi.generator.core.generator

import dev.banking.asyncapi.generator.core.generator.artifact.AvroSchemaArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.artifact.DocumentArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.artifact.JsonSchemaArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.artifact.ModelArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.artifact.NativeAvroArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.artifact.NativeProtobufArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.input.GenerationInputCompatibilityValidator
import dev.banking.asyncapi.generator.core.generator.input.GenerationInputFactory
import dev.banking.asyncapi.generator.core.generator.kafka.spring.SpringKafkaClientGeneration
import dev.banking.asyncapi.generator.core.generator.output.FileSystemGeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationPlanner
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedGenerationCapability

/**
 * Coordinates generator input preparation, planning, rendering, and artifact writing.
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
        val generationPlan = generationPlanner.plan(generatorConfiguration)
        val documentTasks = generationPlan.tasks.filterIsInstance<GenerationTask.DocumentArtifact>()
        val result =
            if (documentTasks.size == generationPlan.tasks.size) {
                renderDocumentArtifacts(documentTasks, asyncApiDocument)
            } else {
                val generationInput = generationInputFactory.create(asyncApiDocument)
                generationInputCompatibilityValidator.validate(
                    generationInput = generationInput,
                    generationPlan = generationPlan,
                )
                renderArtifacts(generationPlan.tasks, generationInput, asyncApiDocument)
            }
        val artifactWriter =
            FileSystemGeneratedArtifactWriter(
                sourceOutputDirectory = generatorConfiguration.output.sourceOutputDirectory,
                resourceOutputDirectory = generatorConfiguration.output.resourceOutputDirectory,
                javaSourceOutputDirectory = generatorConfiguration.output.javaSourceOutputDirectory,
            )

        artifactWriter.write(result)
    }

    private fun renderDocumentArtifacts(
        tasks: List<GenerationTask.DocumentArtifact>,
        asyncApiDocument: AsyncApiDocument,
    ): GenerationResult =
        tasks.fold(GenerationResult.Empty) { result, task ->
            result +
                documentArtifactGeneration.render(
                    task = task,
                    asyncApiDocument = asyncApiDocument,
                )
        }

    private fun renderArtifacts(
        tasks: List<GenerationTask>,
        generationInput: GenerationInput,
        asyncApiDocument: AsyncApiDocument,
    ): GenerationResult =
        tasks.fold(GenerationResult.Empty) { result, task ->
            result + renderArtifactTask(task, generationInput, asyncApiDocument)
        }

    private fun renderArtifactTask(
        task: GenerationTask,
        generationInput: GenerationInput,
        asyncApiDocument: AsyncApiDocument,
    ): GenerationResult =
        when (task) {
            is GenerationTask.DocumentArtifact ->
                documentArtifactGeneration.render(
                    task = task,
                    asyncApiDocument = asyncApiDocument,
                )
            is GenerationTask.ModelArtifacts ->
                modelArtifactGeneration.renderModelArtifacts(
                    task = task,
                    generationInput = generationInput,
                )
            is GenerationTask.KafkaKeyModelArtifacts ->
                modelArtifactGeneration.renderKafkaKeyModelArtifacts(
                    task = task,
                    generationInput = generationInput,
                )
            is GenerationTask.SpringKafkaClient ->
                springKafkaClientGeneration.render(
                    task = task,
                    generationInput = generationInput,
                )
            is GenerationTask.QuarkusKafkaClient ->
                throw UnsupportedGenerationCapability("Quarkus Kafka client generation")
            is GenerationTask.NativeAvroArtifacts ->
                nativeAvroArtifactGeneration.render(
                    task = task,
                    generationInput = generationInput,
                )
            is GenerationTask.NativeProtobufArtifacts ->
                nativeProtobufArtifactGeneration.render(
                    task = task,
                    generationInput = generationInput,
                )
            is GenerationTask.AvroSchemaArtifacts ->
                avroSchemaArtifactGeneration.render(
                    task = task,
                    generationInput = generationInput,
                )
            is GenerationTask.JsonSchemaArtifacts ->
                jsonSchemaArtifactGeneration.render(
                    task = task,
                    generationInput = generationInput,
                )
        }
}
