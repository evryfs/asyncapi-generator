package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.jsonschema.JsonSchemaGenerator
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask

/**
 * Renders planned JSON Schema artifacts before writing them.
 *
 * Expected behavior is covered by:
 * - `JsonSchemaArtifactGenerationTest`
 */
class JsonSchemaArtifactGeneration(
    private val jsonSchemaGenerator: JsonSchemaGenerator = JsonSchemaGenerator(),
) {
    fun render(
        task: GenerationTask.JsonSchemaArtifacts,
        generationInput: GenerationInput,
    ): GenerationResult =
        jsonSchemaGenerator.render(
            schemas = generationInput.declaredSchemas,
            multiFormatSchemas = generationInput.multiFormatSchemas,
            packageName = task.packageName,
        )
}
