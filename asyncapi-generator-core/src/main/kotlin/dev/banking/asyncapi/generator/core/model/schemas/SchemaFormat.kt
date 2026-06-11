package dev.banking.asyncapi.generator.core.model.schemas

import dev.banking.asyncapi.generator.core.constants.AsyncApiConstants

/**
 * Known AsyncAPI `schemaFormat` values grouped by their semantic schema family.
 *
 * The parser can use this type to distinguish JSON-compatible AsyncAPI schema objects
 * from native schema formats such as Avro and Protobuf before generation decides which
 * outputs can consume each payload format.
 *
 * Expected behavior is covered by:
 * - `SchemaFormatTest`
 * - `MultiFormatSchemaParserTest`
 */
enum class SchemaFormat(
    val value: String,
    val family: SchemaFormatFamily,
) {
    ASYNCAPI_3_0_0(
        value = AsyncApiConstants.ASYNCAPI_V_3_0_0,
        family = SchemaFormatFamily.ASYNCAPI,
    ),
    ASYNCAPI_3_0_0_JSON(
        value = AsyncApiConstants.ASYNCAPI_V_3_0_0_JSON,
        family = SchemaFormatFamily.ASYNCAPI,
    ),
    ASYNCAPI_3_0_0_YAML(
        value = AsyncApiConstants.ASYNCAPI_V_3_0_0_YAML,
        family = SchemaFormatFamily.ASYNCAPI,
    ),
    JSON_SCHEMA_DRAFT_07_JSON(
        value = AsyncApiConstants.JSON_SCHEMA_DRAFT_07_JSON,
        family = SchemaFormatFamily.JSON_SCHEMA_DRAFT_07,
    ),
    JSON_SCHEMA_DRAFT_07_YAML(
        value = AsyncApiConstants.JSON_SCHEMA_DRAFT_07_YAML,
        family = SchemaFormatFamily.JSON_SCHEMA_DRAFT_07,
    ),
    AVRO_1_9_0(
        value = AsyncApiConstants.AVRO_V_1_9_0,
        family = SchemaFormatFamily.AVRO,
    ),
    AVRO_1_9_0_JSON(
        value = AsyncApiConstants.AVRO_V_1_9_0_JSON,
        family = SchemaFormatFamily.AVRO,
    ),
    AVRO_1_9_0_YAML(
        value = AsyncApiConstants.AVRO_V_1_9_0_YAML,
        family = SchemaFormatFamily.AVRO,
    ),
    OPENAPI_3_0_0(
        value = AsyncApiConstants.OPENAPI_V_3_0_0,
        family = SchemaFormatFamily.OPENAPI,
    ),
    OPENAPI_3_0_0_JSON(
        value = AsyncApiConstants.OPENAPI_V_3_0_0_JSON,
        family = SchemaFormatFamily.OPENAPI,
    ),
    OPENAPI_3_0_0_YAML(
        value = AsyncApiConstants.OPENAPI_V_3_0_0_YAML,
        family = SchemaFormatFamily.OPENAPI,
    ),
    RAML_1_0_YAML(
        value = AsyncApiConstants.RAML_V_1_0_YAML,
        family = SchemaFormatFamily.RAML,
    ),
    PROTOBUF_2(
        value = AsyncApiConstants.PROTOBUF_V_2,
        family = SchemaFormatFamily.PROTOBUF,
    ),
    PROTOBUF_3(
        value = AsyncApiConstants.PROTOBUF_V_3,
        family = SchemaFormatFamily.PROTOBUF,
    );

    val isAsyncApiSchemaObject: Boolean
        get() = family == SchemaFormatFamily.ASYNCAPI

    val isNativeAvro: Boolean
        get() = family == SchemaFormatFamily.AVRO

    val isNativeProtobuf: Boolean
        get() = family == SchemaFormatFamily.PROTOBUF

    companion object {
        private val byValue = entries.associateBy(SchemaFormat::value)

        fun fromValue(value: String): SchemaFormat? = byValue[value]
    }
}

/**
 * Broad semantic family for a known `schemaFormat` value.
 */
enum class SchemaFormatFamily {
    ASYNCAPI,
    JSON_SCHEMA_DRAFT_07,
    AVRO,
    OPENAPI,
    RAML,
    PROTOBUF,
}
