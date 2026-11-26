package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.jsonschema.JsonSchemaGenerator
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.generator.task.GenerationContext
import dev.banking.asyncapi.generator.core.generator.task.TaskRenderer
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.MissingSchemaGenerationInput
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedSchemaGenerationInput

/**
 * Renders planned JSON Schema artifacts from AsyncAPI Schema Objects, Boolean schemas, and native
 * JSON Schema Draft 07 schemas; any other native format fails.
 *
 * @param jsonSchemaGenerator writes the JSON Schema files
 */
internal class JsonSchemaArtifactGeneration(
    private val jsonSchemaGenerator: JsonSchemaGenerator = JsonSchemaGenerator(),
) : TaskRenderer<GenerationTask.JsonSchemaArtifacts> {
    override fun validate(
        task: GenerationTask.JsonSchemaArtifacts,
        context: GenerationContext,
    ) {
        val generationInput = context.input
        val incompatibleSchema =
            generationInput.multiFormatSchemas.entries
                .firstOrNull { (_, schema) -> !schema.format.isJsonSchemaDraft07 }
        if (incompatibleSchema != null) {
            throw UnsupportedSchemaGenerationInput(
                output = "JSON Schema generation",
                payloadName = incompatibleSchema.key,
                inputFormat = "schemaFormat '${incompatibleSchema.value.schemaFormat}'",
                supportedInput = SUPPORTED_INPUT,
            )
        }
        val declarations = generationInput.schemaDeclarations
        if (
            declarations.asyncApiSchemas.isEmpty() &&
            declarations.multiFormatSchemas.isEmpty() &&
            declarations.booleanSchemas.isEmpty()
        ) {
            throw MissingSchemaGenerationInput(output = "JSON Schema generation", supportedInput = SUPPORTED_INPUT)
        }
    }

    override fun render(
        task: GenerationTask.JsonSchemaArtifacts,
        context: GenerationContext,
    ): GenerationResult =
        jsonSchemaGenerator.render(
            schemaDeclarations = context.input.schemaDeclarations,
            packageName = task.schema.packageName,
        )

    private companion object {
        const val SUPPORTED_INPUT =
            "AsyncAPI Schema Objects, Boolean schemas, and native JSON Schema Draft 07 schemas"
    }
}
