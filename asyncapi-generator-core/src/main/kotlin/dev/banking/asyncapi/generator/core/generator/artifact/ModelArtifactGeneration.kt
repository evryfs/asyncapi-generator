package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.ir.ModelDefinitionFactory
import dev.banking.asyncapi.generator.core.generator.ir.ModelEmitter
import dev.banking.asyncapi.generator.core.generator.java.JavaModelEmitter
import dev.banking.asyncapi.generator.core.generator.kafka.KafkaKeyModelSelector
import dev.banking.asyncapi.generator.core.generator.kotlin.KotlinModelEmitter
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage.JAVA
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage.KOTLIN
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.generator.schema.SchemaDeclarationCatalog

/**
 * Renders planned Kotlin and Java model artifacts before writing them.
 */
internal class ModelArtifactGeneration {
    fun renderModelArtifacts(
        task: GenerationTask.ModelArtifacts,
        generationInput: GenerationInput,
    ): GenerationResult {
        val models =
            ModelDefinitionFactory(
                input = generationInput,
                packageName = task.packageName,
                annotation = task.annotation,
            ).create()
        return emitterFor(task).emit(models)
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
                    schemaDeclarations = SchemaDeclarationCatalog(asyncApiSchemas = keySchemas),
                ),
        )
    }

    private fun emitterFor(task: GenerationTask.ModelArtifacts): ModelEmitter =
        when (task.language) {
            KOTLIN -> KotlinModelEmitter()
            JAVA -> JavaModelEmitter(task.javaModelType)
        }
}
