package dev.banking.asyncapi.generator.core.generator.protobuf

import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidNativeProtobufSchema
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema

/**
 * Renders native Protobuf `schemaFormat` payloads into `.proto` artifacts.
 *
 * Expected behavior is covered by:
 * - `NativeProtobufGeneratorTest`
 */
class NativeProtobufGenerator {
    private val packageRegex = Regex("""(?m)^\s*package\s+([A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*)\s*;""")

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
        val content = schemaContent(payloadName, schema)
        val namespace = packageRegex.find(content)?.groupValues?.get(1).orEmpty()

        return GeneratedArtifact(
            relativePath =
                GeneratedArtifactPaths.fromNamespace(
                    namespace = namespace,
                    fileName = "$payloadName.proto",
                ),
            content = content.trimEnd() + System.lineSeparator(),
            kind = GeneratedArtifactKind.SCHEMA,
        )
    }

    private fun schemaContent(
        payloadName: String,
        schema: MultiFormatSchema,
    ): String {
        val content =
            schema.schema as? String
                ?: throw InvalidNativeProtobufSchema(
                    payloadName = payloadName,
                    schemaFormat = schema.schemaFormat,
                    reason = "Native Protobuf schemas must be provided as .proto text.",
                )

        if (content.isBlank()) {
            throw InvalidNativeProtobufSchema(
                payloadName = payloadName,
                schemaFormat = schema.schemaFormat,
                reason = "Native Protobuf schema content cannot be blank.",
            )
        }

        return content
    }
}
