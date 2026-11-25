package dev.banking.asyncapi.generator.core.generator.protobuf

import dev.banking.asyncapi.generator.core.generator.messaging.ResolvedMessagePayload
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedNativeProtobufPayloadType

/**
 * Resolves native Protobuf message payloads to generated JVM type names.
 *
 * Native Protobuf message payloads should use the Java package declared by
 * `option java_package` when present, otherwise the Protobuf package. Runtime
 * client generation currently requires `option java_multiple_files = true` so
 * top-level messages can be referenced directly by generated Kafka APIs.
 *
 * @param schemaParser Parses the native Protobuf schema.
 */
internal class NativeProtobufPayloadTypeResolver(
    private val schemaParser: NativeProtobufSchemaParser = NativeProtobufSchemaParser(),
) {
    fun resolve(payload: ResolvedMessagePayload.MultiFormat): NativeProtobufPayloadType? {
        if (!payload.schema.format.isNativeProtobuf) {
            return null
        }

        val protobufSchema = schemaParser.parse(payload.typeName, payload.schema)
        val packageName = protobufSchema.javaPackageName ?: protobufSchema.protoPackageName
        if (packageName.isNullOrBlank()) {
            throw unsupported(
                payload = payload,
                reason =
                    "Protobuf client APIs require either `option java_package = \"...\";` " +
                        "or a `package ...;` declaration.",
            )
        }

        val reason = clientTypeViolation(protobufSchema, payload.typeName)
        if (reason != null) throw unsupported(payload = payload, reason = reason)

        return NativeProtobufPayloadType(
            typeName = payload.typeName,
            packageName = packageName,
        )
    }

    private fun clientTypeViolation(
        protobufSchema: NativeProtobufSchema,
        typeName: String,
    ): String? =
        when {
            protobufSchema.javaMultipleFiles != true ->
                "Protobuf client APIs require `option java_multiple_files = true;` " +
                    "so the payload message can be referenced as a top-level Java type."
            typeName !in protobufSchema.messageNames ->
                "Protobuf client APIs require a top-level message named '$typeName'."
            else -> null
        }

    private fun unsupported(
        payload: ResolvedMessagePayload.MultiFormat,
        reason: String,
    ): UnsupportedNativeProtobufPayloadType =
        UnsupportedNativeProtobufPayloadType(
            payloadName = payload.typeName,
            schemaFormat = payload.schema.schemaFormat,
            reason = reason,
        )
}

/**
 * The JVM type generated for a native Protobuf payload.
 *
 * @param typeName Simple name of the type.
 * @param packageName Java package of the type.
 */
internal data class NativeProtobufPayloadType(
    val typeName: String,
    val packageName: String,
)
