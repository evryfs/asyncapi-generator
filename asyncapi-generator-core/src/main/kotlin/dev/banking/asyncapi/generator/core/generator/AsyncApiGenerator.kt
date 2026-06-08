package dev.banking.asyncapi.generator.core.generator

import dev.banking.asyncapi.generator.core.generator.avro.AvroGenerator
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.input.GenerationInputFactory
import dev.banking.asyncapi.generator.core.generator.java.JavaGenerator
import dev.banking.asyncapi.generator.core.generator.java.JavaModelPreparer
import dev.banking.asyncapi.generator.core.generator.kafka.spring.SpringKafkaClientGeneration
import dev.banking.asyncapi.generator.core.generator.kotlin.KotlinGenerator
import dev.banking.asyncapi.generator.core.generator.kotlin.KotlinModelPreparer
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName.JAVA
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName.KOTLIN
import dev.banking.asyncapi.generator.core.generator.model.GeneratorOptions
import dev.banking.asyncapi.generator.core.generator.output.FileSystemGeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactWriter
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
    private val kotlinModelPreparer = KotlinModelPreparer()
    private val javaModelPreparer = JavaModelPreparer()
    private val generationPlanner = GenerationPlanner()
    private val springKafkaClientGeneration = SpringKafkaClientGeneration()

    fun generate(
        asyncApiDocument: AsyncApiDocument,
        generatorOptions: GeneratorOptions,
    ) {
        val generationInput = generationInputFactory.create(asyncApiDocument)
        val generationPlan = generationPlanner.plan(generatorOptions)
        val artifactWriter =
            FileSystemGeneratedArtifactWriter(
                sourceOutputDirectory = generatorOptions.codegenOutputDirectory,
                resourceOutputDirectory = generatorOptions.resourceOutputDirectory,
            )

        generationPlan.tasks.forEach { task ->
            when (task) {
                is GenerationTask.ModelArtifacts ->
                    writeModelArtifacts(
                        task = task,
                        generationInput = generationInput,
                        generatorOptions = generatorOptions,
                        artifactWriter = artifactWriter,
                    )
                is GenerationTask.HeaderModelArtifacts ->
                    writeHeaderModelArtifacts(
                        task = task,
                        asyncApiDocument = asyncApiDocument,
                        generationInput = generationInput,
                        generatorOptions = generatorOptions,
                        artifactWriter = artifactWriter,
                    )
                is GenerationTask.SpringKafkaClient ->
                    springKafkaClientGeneration.generate(
                        task = task,
                        generationInput = generationInput,
                        sourceOutputDirectory = generatorOptions.codegenOutputDirectory,
                        resourceOutputDirectory = generatorOptions.resourceOutputDirectory,
                    )
                is GenerationTask.QuarkusKafkaClient ->
                    log.info("Generate ${task.language.name.titlecase()} Quarkus Kafka Client is not yet implemented. Skipping..")
                is GenerationTask.AvroSchemaArtifacts ->
                    writeAvroSchemaArtifacts(
                        task = task,
                        generationInput = generationInput,
                        generatorOptions = generatorOptions,
                        artifactWriter = artifactWriter,
                    )
            }
        }
    }

    private fun writeModelArtifacts(
        task: GenerationTask.ModelArtifacts,
        generationInput: GenerationInput,
        generatorOptions: GeneratorOptions,
        artifactWriter: GeneratedArtifactWriter,
    ) {
        when (task.language) {
            KOTLIN -> {
                val generationModel =
                    kotlinModelPreparer.prepare(
                        input = generationInput,
                        packageName = task.packageName,
                        annotation = task.annotation,
                    )
                val generator =
                    KotlinGenerator(
                        packageName = task.packageName,
                        outputDir = generatorOptions.codegenOutputDirectory,
                        generationModel = generationModel,
                    )
                artifactWriter.write(generator.render())
            }
            JAVA -> {
                val generationModel =
                    javaModelPreparer.prepare(
                        input = generationInput,
                        packageName = task.packageName,
                    )
                val generator =
                    JavaGenerator(
                        packageName = task.packageName,
                        outputDir = generatorOptions.codegenOutputDirectory,
                        generationModel = generationModel,
                    )
                artifactWriter.write(generator.render())
            }
        }
    }

    private fun writeHeaderModelArtifacts(
        task: GenerationTask.HeaderModelArtifacts,
        asyncApiDocument: AsyncApiDocument,
        generationInput: GenerationInput,
        generatorOptions: GeneratorOptions,
        artifactWriter: GeneratedArtifactWriter,
    ) {
        when (task.language) {
            KOTLIN -> {
                val headerModels =
                    kotlinModelPreparer.prepareHeaders(
                        input = generationInput,
                        asyncApiDocument = asyncApiDocument,
                        packageName = task.packageName,
                    )
                if (headerModels.isNotEmpty()) {
                    val generator =
                        KotlinGenerator(
                            packageName = task.packageName,
                            outputDir = generatorOptions.codegenOutputDirectory,
                            generationModel = headerModels,
                        )
                    artifactWriter.write(generator.render())
                }
            }
            JAVA -> {
                val headerModels =
                    javaModelPreparer.prepareHeaders(
                        input = generationInput,
                        asyncApiDocument = asyncApiDocument,
                        packageName = task.packageName,
                    )
                if (headerModels.isNotEmpty()) {
                    val generator =
                        JavaGenerator(
                            packageName = task.packageName,
                            outputDir = generatorOptions.codegenOutputDirectory,
                            generationModel = headerModels,
                        )
                    artifactWriter.write(generator.render())
                }
            }
        }
    }

    private fun writeAvroSchemaArtifacts(
        task: GenerationTask.AvroSchemaArtifacts,
        generationInput: GenerationInput,
        generatorOptions: GeneratorOptions,
        artifactWriter: GeneratedArtifactWriter,
    ) {
        val avroGenerator =
            AvroGenerator(
                outputDir = generatorOptions.resourceOutputDirectory,
                packageName = task.packageName,
            )
        artifactWriter.write(avroGenerator.render(generationInput.schemas))
    }

    private fun String.titlecase(): String =
        lowercase().replaceFirstChar { it.titlecase() }
}
