package dev.banking.asyncapi.generator.core.model.schemas

/**
 * Known AsyncAPI `schemaFormat` values grouped by their semantic schema family.
 *
 * The parser can use this type to distinguish JSON-compatible AsyncAPI schema objects
 * from native schema formats such as Avro and Protobuf before generation decides which
 * outputs can consume each payload format.
 */
enum class SchemaFormat(
    val value: String,
    val family: SchemaFormatFamily,
) {
    ASYNCAPI_3_0_0(
        value = "application/vnd.aai.asyncapi;version=3.0.0",
        family = SchemaFormatFamily.ASYNCAPI,
    ),
    ASYNCAPI_3_0_0_JSON(
        value = "application/vnd.aai.asyncapi+json;version=3.0.0",
        family = SchemaFormatFamily.ASYNCAPI,
    ),
    ASYNCAPI_3_0_0_YAML(
        value = "application/vnd.aai.asyncapi+yaml;version=3.0.0",
        family = SchemaFormatFamily.ASYNCAPI,
    ),
    JSON_SCHEMA_DRAFT_07_JSON(
        value = "application/schema+json;version=draft-07",
        family = SchemaFormatFamily.JSON_SCHEMA_DRAFT_07,
    ),
    JSON_SCHEMA_DRAFT_07_YAML(
        value = "application/schema+yaml;version=draft-07",
        family = SchemaFormatFamily.JSON_SCHEMA_DRAFT_07,
    ),
    AVRO_1_9_0(
        value = "application/vnd.apache.avro;version=1.9.0",
        family = SchemaFormatFamily.AVRO,
    ),
    AVRO_1_9_0_JSON(
        value = "application/vnd.apache.avro+json;version=1.9.0",
        family = SchemaFormatFamily.AVRO,
    ),
    AVRO_1_9_0_YAML(
        value = "application/vnd.apache.avro+yaml;version=1.9.0",
        family = SchemaFormatFamily.AVRO,
    ),
    OPENAPI_3_0_0(
        value = "application/vnd.oai.openapi;version=3.0.0",
        family = SchemaFormatFamily.OPENAPI,
    ),
    OPENAPI_3_0_0_JSON(
        value = "application/vnd.oai.openapi+json;version=3.0.0",
        family = SchemaFormatFamily.OPENAPI,
    ),
    OPENAPI_3_0_0_YAML(
        value = "application/vnd.oai.openapi+yaml;version=3.0.0",
        family = SchemaFormatFamily.OPENAPI,
    ),
    RAML_1_0_YAML(
        value = "application/raml+yaml;version=1.0",
        family = SchemaFormatFamily.RAML,
    ),
    PROTOBUF_2(
        value = "application/vnd.google.protobuf;version=2",
        family = SchemaFormatFamily.PROTOBUF,
    ),
    PROTOBUF_3(
        value = "application/vnd.google.protobuf;version=3",
        family = SchemaFormatFamily.PROTOBUF,
    ),
    ;

    val isAsyncApiSchemaObject: Boolean
        get() = family == SchemaFormatFamily.ASYNCAPI

    val isNativeAvro: Boolean
        get() = family == SchemaFormatFamily.AVRO

    val isNativeProtobuf: Boolean
        get() = family == SchemaFormatFamily.PROTOBUF

    val isJsonSchemaDraft07: Boolean
        get() = family == SchemaFormatFamily.JSON_SCHEMA_DRAFT_07

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
