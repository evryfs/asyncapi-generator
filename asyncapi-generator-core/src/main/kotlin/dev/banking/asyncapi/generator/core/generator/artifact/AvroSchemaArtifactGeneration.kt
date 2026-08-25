package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.avro.AvroGenerator
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask

/**
 * Renders planned Avro schema artifacts before writing them.
 */
class AvroSchemaArtifactGeneration {
    fun render(
        task: GenerationTask.AvroSchemaArtifacts,
        generationInput: GenerationInput,
    ): GenerationResult {
        val avroGenerator =
            AvroGenerator(
                packageName = task.packageName,
            )
        return avroGenerator.render(generationInput.schemas)
    }
}
