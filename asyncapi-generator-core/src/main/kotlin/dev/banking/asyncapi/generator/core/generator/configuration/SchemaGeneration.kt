package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Typed schema artifact generation configuration.
 */
sealed interface SchemaGeneration {
    /**
     * Projects AsyncAPI Schema Objects to `.avsc` files.
     *
     * @param packageName the package the schema files are written under.
     */
    data class AvroProjection(
        val packageName: String,
    ) : SchemaGeneration

    /**
     * Copies native Avro payload schemas, optionally with SpecificRecord models.
     *
     * @param generateSpecificRecords whether Avro SpecificRecord models are generated.
     * @param modelPackageName the package of the generated models.
     * @param schemaPackageName the package the schema files are written under.
     */
    data class NativeAvro(
        val generateSpecificRecords: Boolean = true,
        val modelPackageName: String? = null,
        val schemaPackageName: String? = null,
    ) : SchemaGeneration

    /**
     * Copies native Protobuf payload schemas, optionally with compiled models.
     *
     * @param models the Protobuf models to compile, or `null` for schemas only.
     * @param schemaPackageName the package the schema files are written under.
     */
    data class NativeProtobuf(
        val models: ProtobufModelGeneration? = null,
        val schemaPackageName: String? = null,
    ) : SchemaGeneration

    /**
     * Writes Draft 07 JSON Schema files for AsyncAPI and native JSON schemas.
     *
     * @param packageName the package the schema files are written under.
     */
    data class JsonSchema(
        val packageName: String,
    ) : SchemaGeneration
}
