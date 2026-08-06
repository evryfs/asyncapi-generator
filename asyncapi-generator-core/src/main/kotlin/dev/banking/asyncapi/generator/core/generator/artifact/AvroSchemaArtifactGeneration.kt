package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.avro.AvroGenerator
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask

/**
 * Renders planned Avro schema artifacts before writing them.
 *
 * Expected behavior is covered by:
 * - `AvroSchemaArtifactGenerationTest`
 */
class AvroSchemaArtifactGeneration {
    fun generate(
        task: GenerationTask.AvroSchemaArtifacts,
        generationInput: GenerationInput,
        artifactWriter: GeneratedArtifactWriter,
    ) {
        val avroGenerator =
            AvroGenerator(
                packageName = task.packageName,
            )
        artifactWriter.write(avroGenerator.render(generationInput.schemas))
    }
}
