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
internal class NativeProtobufSchemaParser {
    private val protoPackageRegex =
        Regex("""(?m)^\s*package\s+([A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*)\s*;""")
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
            javaMultipleFiles =
                javaMultipleFilesRegex
                    .find(content)
                    ?.groupValues
                    ?.get(1)
                    ?.toBooleanStrict(),
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
            val topLevelName =
                TOP_LEVEL_MESSAGE_REGEX
                    .find(line)
                    ?.groupValues
                    ?.get(1)
                    ?.takeIf { depth == 0 }
            topLevelName?.let(names::add)
            depth = if (topLevelName != null && line.contains('{')) 1 else depthAfter(line, depth)
        }
        return names
    }

    /** The brace depth after [line], starting from [depth]; a closing brace never goes below zero. */
    private fun depthAfter(
        line: String,
        depth: Int,
    ): Int =
        line.fold(depth) { current, c ->
            when (c) {
                '{' -> current + 1
                '}' -> if (current > 0) current - 1 else current
                else -> current
            }
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

/**
 * The declarations of a `.proto` file that generation needs.
 *
 * @property content the file content as written
 * @property protoPackageName the `package` declaration; null without one
 * @property javaPackageName the `java_package` option; null without one
 * @property javaMultipleFiles the `java_multiple_files` option; null when not set
 * @property messageNames the top-level message names, in file order
 */
internal data class NativeProtobufSchema(
    val content: String,
    val protoPackageName: String?,
    val javaPackageName: String?,
    val javaMultipleFiles: Boolean?,
    val messageNames: List<String>,
)
