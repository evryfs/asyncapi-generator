package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.avro.NativeAvroGenerator
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask

/**
 * Renders planned native Avro artifacts before writing them.
 *
 * Expected behavior is covered by:
 * - `NativeAvroArtifactGenerationTest`
 */
class NativeAvroArtifactGeneration {
    private val nativeAvroGenerator = NativeAvroGenerator()

    fun generate(
        task: GenerationTask.NativeAvroArtifacts,
        generationInput: GenerationInput,
        artifactWriter: GeneratedArtifactWriter,
    ) {
        val result =
            nativeAvroGenerator.render(
                schemas = generationInput.multiFormatSchemas,
                generateSpecificRecords = task.generateSpecificRecords,
            ).inSchemaPackage(task.schemaPackageName)

        artifactWriter.write(
            result,
        )
    }
}
