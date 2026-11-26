package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.avro.NativeAvroGenerator
import dev.banking.asyncapi.generator.core.generator.avro.NativeAvroSchemaParser
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.input.PayloadFormatChecks
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.generator.task.GenerationContext
import dev.banking.asyncapi.generator.core.generator.task.TaskRenderer
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.NativeAvroModelPackageMismatch

/**
 * Renders planned native Avro artifacts. SpecificRecords are generated in the namespace of their
 * schema, which must therefore equal the configured model package.
 */
internal class NativeAvroArtifactGeneration : TaskRenderer<GenerationTask.NativeAvroArtifacts> {
    private val nativeAvroGenerator = NativeAvroGenerator()
    private val nativeAvroSchemaParser = NativeAvroSchemaParser()

    override fun validate(
        task: GenerationTask.NativeAvroArtifacts,
        context: GenerationContext,
    ) {
        if (
            task.schema.generateSpecificRecords ||
            !context.plans<GenerationTask.AvroSchemaArtifacts>() ||
            context.input.schemas.isEmpty()
        ) {
            PayloadFormatChecks.requireNativeSchema(
                output = "Native Avro generation",
                generationInput = context.input,
                supportedInput = "native Avro schemas",
                isSupported = { schema -> schema.format.isNativeAvro },
            )
        }
        if (task.schema.generateSpecificRecords && task.schema.modelPackageName != null) {
            requireModelPackage(context.input, task.schema.modelPackageName)
        }
    }

    override fun render(
        task: GenerationTask.NativeAvroArtifacts,
        context: GenerationContext,
    ): GenerationResult =
        nativeAvroGenerator
            .render(
                schemas = context.input.multiFormatSchemas,
                generateSpecificRecords = task.schema.generateSpecificRecords,
            ).inSchemaPackage(task.schema.schemaPackageName)

    private fun requireModelPackage(
        generationInput: GenerationInput,
        configuredPackage: String,
    ) {
        generationInput.multiFormatSchemas
            .filterValues { schema -> schema.format.isNativeAvro }
            .forEach { (payloadName, schema) ->
                val schemaNamespace = nativeAvroSchemaParser.parse(payloadName, schema).namespace
                if (schemaNamespace != configuredPackage) {
                    throw NativeAvroModelPackageMismatch(
                        payloadName = payloadName,
                        configuredPackage = configuredPackage,
                        schemaNamespace = schemaNamespace,
                    )
                }
            }
    }
}
