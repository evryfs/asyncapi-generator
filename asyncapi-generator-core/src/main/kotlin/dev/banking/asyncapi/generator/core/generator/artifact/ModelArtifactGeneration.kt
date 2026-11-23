package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage.JAVA
import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage.KOTLIN
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.ir.ModelDefinitionFactory
import dev.banking.asyncapi.generator.core.generator.ir.ModelEmitter
import dev.banking.asyncapi.generator.core.generator.java.JavaModelEmitter
import dev.banking.asyncapi.generator.core.generator.java.JavaModelType
import dev.banking.asyncapi.generator.core.generator.kafka.KafkaKeyModelSelector
import dev.banking.asyncapi.generator.core.generator.kotlin.KotlinModelEmitter
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
                packageName = task.models.packageName,
                annotation = task.models.annotation,
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
                    ModelGeneration.Enabled(packageName = task.packageName, modelType = task.language.defaultModelType),
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
            JAVA -> JavaModelEmitter(JavaModelType.of(task.models.modelType))
        }
}
