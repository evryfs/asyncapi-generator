package dev.banking.asyncapi.generator.core.generator.avro

import dev.banking.asyncapi.generator.core.generator.messaging.ResolvedMessagePayload
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedNativeAvroPayloadType
import org.apache.avro.Schema

/**
 * Resolves native Avro message payloads to generated JVM type names.
 *
 * Native Avro message payloads should use the package declared by the Avro
 * namespace, not the AsyncAPI JSON model package.
 */
internal class NativeAvroPayloadTypeResolver(
    private val schemaParser: NativeAvroSchemaParser = NativeAvroSchemaParser(),
) {
    fun resolve(payload: ResolvedMessagePayload.MultiFormat): NativeAvroPayloadType? {
        if (!payload.schema.format.isNativeAvro) {
            return null
        }

        val avroSchema = schemaParser.parse(payload.typeName, payload.schema)
        if (!avroSchema.supportsGeneratedType()) {
            throw UnsupportedNativeAvroPayloadType(
                payloadName = payload.typeName,
                schemaFormat = payload.schema.schemaFormat,
                schemaType = avroSchema.type.name,
            )
        }

        val packageName = avroSchema.namespace.orEmpty().ifBlank { null }
        return NativeAvroPayloadType(
            typeName = avroSchema.name,
            packageName = packageName,
            importName = packageName?.let { "$it.${avroSchema.name}" },
        )
    }

    private fun Schema.supportsGeneratedType(): Boolean =
        type == Schema.Type.RECORD ||
            type == Schema.Type.ENUM ||
            type == Schema.Type.FIXED
}

internal data class NativeAvroPayloadType(
    val typeName: String,
    val packageName: String?,
    val importName: String?,
)
