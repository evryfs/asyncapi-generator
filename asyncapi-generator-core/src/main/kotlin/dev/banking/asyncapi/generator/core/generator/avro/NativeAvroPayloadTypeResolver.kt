package dev.banking.asyncapi.generator.core.generator.avro

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMultiFormatMessage
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedNativeAvroPayloadType
import org.apache.avro.Schema

/**
 * Resolves native Avro message payloads to generated JVM type names.
 *
 * Native Avro message payloads should use the package declared by the Avro
 * namespace, not the AsyncAPI JSON model package.
 */
class NativeAvroPayloadTypeResolver(
    private val schemaParser: NativeAvroSchemaParser = NativeAvroSchemaParser(),
) {
    fun resolve(message: AnalyzedMultiFormatMessage): NativeAvroPayloadType? {
        if (!message.schema.format.isNativeAvro) {
            return null
        }

        val avroSchema = schemaParser.parse(message.payloadName, message.schema)
        if (!avroSchema.supportsGeneratedType()) {
            throw UnsupportedNativeAvroPayloadType(
                payloadName = message.payloadName,
                schemaFormat = message.schema.schemaFormat,
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

data class NativeAvroPayloadType(
    val typeName: String,
    val packageName: String?,
    val importName: String?,
)
