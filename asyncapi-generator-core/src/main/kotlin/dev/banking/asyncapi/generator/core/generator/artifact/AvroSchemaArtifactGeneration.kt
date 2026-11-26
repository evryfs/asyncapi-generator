package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.avro.AvroGenerator
import dev.banking.asyncapi.generator.core.generator.input.SourceSchemaCompatibilityValidator
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.generator.task.GenerationContext
import dev.banking.asyncapi.generator.core.generator.task.TaskRenderer
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedPayloadSchemaFormat

/**
 * Renders planned Avro Projection artifacts. Native Avro and Protobuf payloads need their own
 * native output in the same plan, as Avro Projection reads only AsyncAPI Schema Objects.
 */
internal class AvroSchemaArtifactGeneration : TaskRenderer<GenerationTask.AvroSchemaArtifacts> {
    override fun validate(
        task: GenerationTask.AvroSchemaArtifacts,
        context: GenerationContext,
    ) {
        val hasNativeAvro = context.plans<GenerationTask.NativeAvroArtifacts>()
        val hasNativeProtobuf = context.plans<GenerationTask.NativeProtobufArtifacts>()
        val firstUnsupportedSchema =
            context.input.multiFormatSchemas.entries.firstOrNull { (_, schema) ->
                when {
                    schema.format.isNativeAvro -> !hasNativeAvro
                    schema.format.isNativeProtobuf -> !hasNativeProtobuf
                    else -> true
                }
            }
        if (firstUnsupportedSchema != null) {
            throw UnsupportedPayloadSchemaFormat(
                output = "Avro Projection",
                payloadName = firstUnsupportedSchema.key,
                schemaFormat = firstUnsupportedSchema.value.schemaFormat,
            )
        }
        SourceSchemaCompatibilityValidator.validate(
            output = "Avro projection",
            schemas = context.input.schemas,
            checkStructuralModels = true,
            checkJavaPatterns = false,
        )
    }

    override fun render(
        task: GenerationTask.AvroSchemaArtifacts,
        context: GenerationContext,
    ): GenerationResult = AvroGenerator(packageName = task.schema.packageName).render(context.input)
}
