package dev.banking.asyncapi.generator.core.generator.protobuf

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMultiFormatMessage
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedNativeProtobufPayloadType

/**
 * Resolves native Protobuf message payloads to generated JVM type names.
 *
 * Native Protobuf message payloads should use the Java package declared by
 * `option java_package` when present, otherwise the Protobuf package. Runtime
 * client generation currently requires `option java_multiple_files = true` so
 * top-level messages can be referenced directly by generated Kafka APIs.
 *
 * Expected behavior is covered by:
 * - `NativeProtobufPayloadTypeResolverTest`
 */
class NativeProtobufPayloadTypeResolver(
    private val schemaParser: NativeProtobufSchemaParser = NativeProtobufSchemaParser(),
) {
    fun resolve(message: AnalyzedMultiFormatMessage): NativeProtobufPayloadType? {
        if (!message.schema.format.isNativeProtobuf) {
            return null
        }

        val protobufSchema = schemaParser.parse(message.payloadName, message.schema)
        val packageName = protobufSchema.javaPackageName ?: protobufSchema.protoPackageName
        if (packageName.isNullOrBlank()) {
            throw unsupported(
                message = message,
                reason = "Protobuf client APIs require either `option java_package = \"...\";` or a `package ...;` declaration.",
            )
        }

        if (protobufSchema.javaMultipleFiles != true) {
            throw unsupported(
                message = message,
                reason = "Protobuf client APIs require `option java_multiple_files = true;` so the payload message can be referenced as a top-level Java type.",
            )
        }

        if (message.payloadName !in protobufSchema.messageNames) {
            throw unsupported(
                message = message,
                reason = "Protobuf client APIs require a top-level message named '${message.payloadName}'.",
            )
        }

        return NativeProtobufPayloadType(
            typeName = message.payloadName,
            packageName = packageName,
            importName = "$packageName.${message.payloadName}",
        )
    }

    private fun unsupported(
        message: AnalyzedMultiFormatMessage,
        reason: String,
    ): UnsupportedNativeProtobufPayloadType =
        UnsupportedNativeProtobufPayloadType(
            payloadName = message.payloadName,
            schemaFormat = message.schema.schemaFormat,
            reason = reason,
        )
}

data class NativeProtobufPayloadType(
    val typeName: String,
    val packageName: String,
    val importName: String,
)
