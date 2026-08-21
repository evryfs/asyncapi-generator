package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.java.JavaGenerator
import dev.banking.asyncapi.generator.core.generator.java.JavaModelPreparer
import dev.banking.asyncapi.generator.core.generator.kafka.KafkaKeyModelSelector
import dev.banking.asyncapi.generator.core.generator.kotlin.KotlinGenerator
import dev.banking.asyncapi.generator.core.generator.kotlin.KotlinModelPreparer
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage.JAVA
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage.KOTLIN
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask

/**
 * Renders planned Kotlin and Java model artifacts before writing them.
 */
class ModelArtifactGeneration(
    private val kotlinModelPreparer: KotlinModelPreparer = KotlinModelPreparer(),
    private val javaModelPreparer: JavaModelPreparer = JavaModelPreparer(),
) {
    fun renderModelArtifacts(
        task: GenerationTask.ModelArtifacts,
        generationInput: GenerationInput,
    ): GenerationResult =
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
                generator.render()
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
                generator.render()
            }
        }

    fun renderKafkaKeyModelArtifacts(
        task: GenerationTask.KafkaKeyModelArtifacts,
        generationInput: GenerationInput,
    ): GenerationResult {
        val keySchemas = KafkaKeyModelSelector.select(generationInput)
        if (keySchemas.isEmpty()) return GenerationResult.Empty

        return renderModelArtifacts(
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
        )
    }
}
