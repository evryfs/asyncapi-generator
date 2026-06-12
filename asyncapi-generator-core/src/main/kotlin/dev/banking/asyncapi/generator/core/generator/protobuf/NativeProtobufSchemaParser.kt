package dev.banking.asyncapi.generator.core.generator.protobuf

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidNativeProtobufSchema
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema

/**
 * Extracts the Protobuf declarations required by native Protobuf generators.
 *
 * The generator does not compile `.proto` files directly, but it needs stable
 * access to the package, Java package, Java file mode, and top-level message
 * declarations before generated Kafka APIs can reference Protobuf types.
 *
 * Expected behavior is covered by:
 * - `NativeProtobufGeneratorTest`
 * - `NativeProtobufPayloadTypeResolverTest`
 */
class NativeProtobufSchemaParser {
    private val protoPackageRegex = Regex("""(?m)^\s*package\s+([A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*)\s*;""")
    private val javaPackageRegex = Regex("""(?m)^\s*option\s+java_package\s*=\s*"([^"]+)"\s*;""")
    private val javaMultipleFilesRegex = Regex("""(?m)^\s*option\s+java_multiple_files\s*=\s*(true|false)\s*;""")
    private val messageRegex = Regex("""(?m)^\s*message\s+([A-Za-z_][A-Za-z0-9_]*)\s*\{""")

    fun parse(
        payloadName: String,
        schema: MultiFormatSchema,
    ): NativeProtobufSchema {
        val content = schemaContent(payloadName, schema)

        return NativeProtobufSchema(
            content = content,
            protoPackageName = protoPackageRegex.find(content)?.groupValues?.get(1),
            javaPackageName = javaPackageRegex.find(content)?.groupValues?.get(1),
            javaMultipleFiles = javaMultipleFilesRegex.find(content)?.groupValues?.get(1)?.toBooleanStrict(),
            messageNames = messageRegex.findAll(content).map { match -> match.groupValues[1] }.toList(),
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

data class NativeProtobufSchema(
    val content: String,
    val protoPackageName: String?,
    val javaPackageName: String?,
    val javaMultipleFiles: Boolean?,
    val messageNames: List<String>,
)
