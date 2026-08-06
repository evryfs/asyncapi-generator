package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.java.JavaGenerator
import dev.banking.asyncapi.generator.core.generator.java.JavaModelPreparer
import dev.banking.asyncapi.generator.core.generator.kafka.KafkaKeyModelSelector
import dev.banking.asyncapi.generator.core.generator.kotlin.KotlinGenerator
import dev.banking.asyncapi.generator.core.generator.kotlin.KotlinModelPreparer
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage.JAVA
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage.KOTLIN
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask

/**
 * Renders planned Kotlin and Java model artifacts before writing them.
 *
 * Expected behavior is covered by:
 * - `ModelArtifactGenerationTest`
 */
class ModelArtifactGeneration(
    private val kotlinModelPreparer: KotlinModelPreparer = KotlinModelPreparer(),
    private val javaModelPreparer: JavaModelPreparer = JavaModelPreparer(),
) {
    fun generateModelArtifacts(
        task: GenerationTask.ModelArtifacts,
        generationInput: GenerationInput,
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
                        generationModel = generationModel,
                    )
                artifactWriter.write(generator.render())
            }
            JAVA -> {
                val generationModel =
                    javaModelPreparer.prepare(
                        input = generationInput,
                        packageName = task.packageName,
                        annotation = task.annotation,
                    )
                val generator =
                    JavaGenerator(
                        packageName = task.packageName,
                        generationModel = generationModel,
                        javaModelType = task.javaModelType,
                    )
                artifactWriter.write(generator.render())
            }
        }
    }

    fun generateKafkaKeyModelArtifacts(
        task: GenerationTask.KafkaKeyModelArtifacts,
        generationInput: GenerationInput,
        artifactWriter: GeneratedArtifactWriter,
    ) {
        val keySchemas = KafkaKeyModelSelector.select(generationInput)
        if (keySchemas.isEmpty()) return

        generateModelArtifacts(
            task =
                GenerationTask.ModelArtifacts(
                    language = task.language,
                    packageName = task.packageName,
                ),
            generationInput =
                generationInput.copy(
                    schemas = keySchemas,
                    declaredSchemas = keySchemas,
                    multiFormatSchemas = emptyMap(),
                ),
            artifactWriter = artifactWriter,
        )
    }

}
