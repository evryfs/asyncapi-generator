package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.avro.NativeAvroGenerator
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask

/**
 * Renders planned native Avro artifacts before writing them.
 */
class NativeAvroArtifactGeneration {
    private val nativeAvroGenerator = NativeAvroGenerator()

    fun render(
        task: GenerationTask.NativeAvroArtifacts,
        generationInput: GenerationInput,
    ): GenerationResult =
        nativeAvroGenerator.render(
            schemas = generationInput.multiFormatSchemas,
            generateSpecificRecords = task.generateSpecificRecords,
        ).inSchemaPackage(task.schemaPackageName)
}
