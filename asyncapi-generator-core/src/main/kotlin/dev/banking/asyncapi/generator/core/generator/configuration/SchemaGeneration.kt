package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Typed schema artifact generation configuration.
 *
 * `AvroProjection` represents AsyncAPI Schema Object to `.avsc` projection.
 * `NativeAvro` represents native Avro `schemaFormat` payload generation.
 * `NativeProtobuf` represents native Protobuf `schemaFormat` payload generation.
 * `JsonSchema` represents AsyncAPI and native Draft 07 JSON Schema artifacts.
 */
sealed interface SchemaGeneration {
    data class AvroProjection(
        val packageName: String,
    ) : SchemaGeneration

    data class NativeAvro(
        val generateSpecificRecords: Boolean = true,
        val modelPackageName: String? = null,
        val schemaPackageName: String? = null,
    ) : SchemaGeneration

    data class NativeProtobuf(
        val models: ProtobufModelGeneration? = null,
        val schemaPackageName: String? = null,
    ) : SchemaGeneration

    data class JsonSchema(
        val packageName: String,
    ) : SchemaGeneration
}
