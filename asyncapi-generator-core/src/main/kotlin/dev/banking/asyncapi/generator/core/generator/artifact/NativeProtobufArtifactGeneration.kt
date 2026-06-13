package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.generator.protobuf.NativeProtobufGenerator

/**
 * Renders planned native Protobuf artifacts before writing them.
 *
 * Expected behavior is covered by:
 * - `NativeProtobufArtifactGenerationTest`
 */
class NativeProtobufArtifactGeneration {
    private val nativeProtobufGenerator = NativeProtobufGenerator()

    fun generate(
        task: GenerationTask.NativeProtobufArtifacts,
        generationInput: GenerationInput,
        artifactWriter: GeneratedArtifactWriter,
    ) {
        artifactWriter.write(
            nativeProtobufGenerator.render(
                schemas = generationInput.multiFormatSchemas,
                generateJavaMessageTypes = task.generateJavaMessageTypes,
            ),
        )
    }
}
