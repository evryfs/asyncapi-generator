package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.input.SourceSchemaCompatibilityValidator
import dev.banking.asyncapi.generator.core.generator.messaging.kafka.KafkaKeyModelSelector
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.generator.schema.SchemaDeclarationCatalog
import dev.banking.asyncapi.generator.core.generator.task.GenerationContext
import dev.banking.asyncapi.generator.core.generator.task.TaskRenderer

/**
 * Renders the models of object Kafka keys, which a Spring Kafka client needs when native Avro or
 * Protobuf models carry the payloads.
 *
 * @param models renders the key models like payload models
 */
internal class KafkaKeyModelArtifactGeneration(
    private val models: ModelArtifactGeneration,
) : TaskRenderer<GenerationTask.KafkaKeyModelArtifacts> {
    override fun validate(
        task: GenerationTask.KafkaKeyModelArtifacts,
        context: GenerationContext,
    ) {
        SourceSchemaCompatibilityValidator.validate(
            output = "Kafka key model generation",
            schemas = KafkaKeyModelSelector.select(context.input),
            checkStructuralModels = true,
            checkJavaPatterns = true,
        )
    }

    override fun render(
        task: GenerationTask.KafkaKeyModelArtifacts,
        context: GenerationContext,
    ): GenerationResult {
        val keySchemas = KafkaKeyModelSelector.select(context.input)
        if (keySchemas.isEmpty()) return GenerationResult.Empty

        return models.render(
            task =
                GenerationTask.ModelArtifacts(
                    ModelGeneration.Enabled(packageName = task.packageName, modelType = task.language.defaultModelType),
                ),
            generationInput =
                context.input.copy(
                    schemas = keySchemas,
                    schemaDeclarations = SchemaDeclarationCatalog(asyncApiSchemas = keySchemas),
                ),
        )
    }
}
