package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.generator.protobuf.NativeProtobufGenerator

/**
 * Renders planned native Protobuf artifacts before writing them.
 */
class NativeProtobufArtifactGeneration {
    private val nativeProtobufGenerator = NativeProtobufGenerator()

    fun render(
        task: GenerationTask.NativeProtobufArtifacts,
        generationInput: GenerationInput,
    ): GenerationResult =
        nativeProtobufGenerator.render(
            schemas = generationInput.multiFormatSchemas,
            models = task.models,
        ).inSchemaPackage(task.schemaPackageName)
}
