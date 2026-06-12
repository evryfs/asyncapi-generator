package dev.banking.asyncapi.generator.core.generator.protobuf

import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema

/**
 * Renders native Protobuf `schemaFormat` payloads into `.proto` artifacts.
 *
 * Expected behavior is covered by:
 * - `NativeProtobufGeneratorTest`
 */
class NativeProtobufGenerator {
    private val schemaParser = NativeProtobufSchemaParser()

    fun render(schemas: Map<String, MultiFormatSchema>): GenerationResult =
        GenerationResult(
            schemas
                .filter { (_, schema) -> schema.format.isNativeProtobuf }
                .map { (payloadName, schema) -> renderSchemaArtifact(payloadName, schema) },
        )

    private fun renderSchemaArtifact(
        payloadName: String,
        schema: MultiFormatSchema,
    ): GeneratedArtifact {
        val parsedSchema = schemaParser.parse(payloadName, schema)
        val namespace = parsedSchema.protoPackageName.orEmpty()

        return GeneratedArtifact(
            relativePath =
                GeneratedArtifactPaths.fromNamespace(
                    namespace = namespace,
                    fileName = "$payloadName.proto",
                ),
            content = parsedSchema.content.trimEnd() + System.lineSeparator(),
            kind = GeneratedArtifactKind.SCHEMA,
        )
    }
}
