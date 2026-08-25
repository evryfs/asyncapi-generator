package dev.banking.asyncapi.generator.core.generator.protobuf

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidNativeProtobufSchema
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema

/**
 * Extracts the Protobuf declarations required by native Protobuf generators.
 *
 * The generator does not compile `.proto` files directly, but it needs stable
 * access to the package, Java package, Java file mode, and top-level message
 * declarations before generated Kafka APIs can reference Protobuf types.
 */
class NativeProtobufSchemaParser {
    private val protoPackageRegex = Regex("""(?m)^\s*package\s+([A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*)\s*;""")
    private val javaPackageRegex = Regex("""(?m)^\s*option\s+java_package\s*=\s*"([^"]+)"\s*;""")
    private val javaMultipleFilesRegex = Regex("""(?m)^\s*option\s+java_multiple_files\s*=\s*(true|false)\s*;""")

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
            messageNames = extractTopLevelMessageNames(content),
        )
    }

    /**
     * Finds top-level message declarations by tracking brace depth line by line.
     * A message is top-level when its `message Name {` declaration appears at brace depth 0.
     */
    private fun extractTopLevelMessageNames(content: String): List<String> {
        val names = mutableListOf<String>()
        var depth = 0

        for (line in content.lines()) {
            if (depth == 0 && TOP_LEVEL_MESSAGE_REGEX.containsMatchIn(line)) {
                val name = requireNotNull(TOP_LEVEL_MESSAGE_REGEX.find(line)).groupValues[1]
                names.add(name)
                if (line.contains('{')) {
                    depth = 1
                    continue
                }
            }
            for (c in line) {
                when (c) {
                    '{' -> depth++
                    '}' -> if (depth > 0) depth--
                }
            }
        }

        return names
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

    private companion object {
        val TOP_LEVEL_MESSAGE_REGEX = Regex("""^\s*message\s+([A-Za-z_][A-Za-z0-9_]*)\s*\{""")
    }
}

data class NativeProtobufSchema(
    val content: String,
    val protoPackageName: String?,
    val javaPackageName: String?,
    val javaMultipleFiles: Boolean?,
    val messageNames: List<String>,
)
