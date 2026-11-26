package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.jvm.JvmNaming
import dev.banking.asyncapi.generator.core.generator.messaging.kafka.KafkaKeyModelSelector
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.generator.schema.SchemaDeclarationCatalog
import dev.banking.asyncapi.generator.core.generator.sourcemodel.ModelDefinitionFactory
import dev.banking.asyncapi.generator.core.generator.sourcetarget.SourceTarget

/**
 * Renders planned payload model artifacts before writing them, with the emitter of their language.
 *
 * @param targetOf the source target of a language
 */
internal class ModelArtifactGeneration(
    private val targetOf: (SourceLanguage) -> SourceTarget,
) {
    fun renderModelArtifacts(
        task: GenerationTask.ModelArtifacts,
        generationInput: GenerationInput,
    ): GenerationResult {
        val models =
            ModelDefinitionFactory(
                input = generationInput,
                packageName = task.models.packageName,
                naming = JvmNaming,
            ).create()
        return targetOf(task.language).modelEmitter(task.models).emit(models)
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
}
