package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.input.PayloadFormatChecks
import dev.banking.asyncapi.generator.core.generator.input.SourceSchemaCompatibilityValidator
import dev.banking.asyncapi.generator.core.generator.jvm.JvmNaming
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.generator.sourcemodel.ModelDefinitionFactory
import dev.banking.asyncapi.generator.core.generator.sourcetarget.SourceTarget
import dev.banking.asyncapi.generator.core.generator.task.GenerationContext
import dev.banking.asyncapi.generator.core.generator.task.TaskRenderer

/**
 * Renders planned payload model artifacts with the emitter of their language.
 *
 * @param targetOf the source target of a language
 */
internal class ModelArtifactGeneration(
    private val targetOf: (SourceLanguage) -> SourceTarget,
) : TaskRenderer<GenerationTask.ModelArtifacts> {
    override fun validate(
        task: GenerationTask.ModelArtifacts,
        context: GenerationContext,
    ) {
        PayloadFormatChecks.rejectMultiFormatSchemas(
            output = "Model generation",
            multiFormatSchemas = context.input.multiFormatSchemas,
        )
        SourceSchemaCompatibilityValidator.validate(
            output = "Model generation",
            schemas = context.input.schemas,
            checkStructuralModels = true,
            checkJavaPatterns = true,
        )
    }

    override fun render(
        task: GenerationTask.ModelArtifacts,
        context: GenerationContext,
    ): GenerationResult = render(task, context.input)

    /** The models of [task] built from [generationInput]. */
    fun render(
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
}
