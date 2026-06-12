package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Typed schema artifact generation configuration.
 *
 * `AvroProjection` represents AsyncAPI Schema Object to `.avsc` projection.
 * `NativeAvro` represents native Avro `schemaFormat` payload generation.
 * `NativeProtobuf` represents native Protobuf `schemaFormat` payload generation.
 *
 * Expected behavior is covered by:
 * - `GenerationPlannerTest`
 */
sealed interface SchemaGeneration {
    data class AvroProjection(
        val packageName: String,
    ) : SchemaGeneration

    data class NativeAvro(
        val generateSpecificRecords: Boolean = true,
    ) : SchemaGeneration

    data object NativeProtobuf : SchemaGeneration
}
