package com.tietoevry.banking.asyncapi.generator.core.parser.schemas

import com.tietoevry.banking.asyncapi.generator.core.constants.AsyncApiConstants
import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import com.tietoevry.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException.UnexpectedSchemaFormat
import com.tietoevry.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException.UnexpectedValue
import com.tietoevry.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException.UnsupportedSchemaFormat

/**
 * [MultiFormatSchemaParser] currently acts as a **validator** for schema format choices.
 *
 * Its primary responsibility is to check the `schemaFormat` string provided in an AsyncAPI document
 * against a predefined list of known formats.
 *
 * **Current State:**
 * - It fully supports and *allows* standard AsyncAPI schema formats (e.g., `application/vnd.aai.asyncapi+yaml`).
 *   When such a format is encountered, it passes validation, and the schema content is then parsed
 *   by the regular [SchemaParser].
 * - For other known formats (e.g., JSON Schema Draft-07, Avro, OpenAPI, RAML, Protobuf),
 *   it *recognizes* the format but **throws a [UnsupportedSchemaFormat]**, indicating that
 *   code generation for these specific formats is not yet implemented in the generator.
 * - For any unknown `schemaFormat` string, it throws an [UnexpectedValue] error.
 *
 * **Future Expansion:**
 * This parser is designed to be expanded in the future to provide full parsing and
 * code generation support for the currently unimplemented schema formats.
 */
class MultiFormatSchemaParser(
    val asyncApiContext: AsyncApiContext,
) {

    fun validateFormat(format: String, path: String) {
        when {
            isAsyncApiFormat(format) -> return
            isJsonSchemaDraftFormat(format) -> throw UnsupportedSchemaFormat(format, path, asyncApiContext)
            isAvroFormat(format) -> throw UnsupportedSchemaFormat(format, path, asyncApiContext)
            isOpenApiFormat(format) -> throw UnsupportedSchemaFormat(format, path, asyncApiContext)
            isRamlFormat(format) -> throw UnsupportedSchemaFormat(format, path, asyncApiContext)
            isProtobufFormat(format) -> throw UnsupportedSchemaFormat(format, path, asyncApiContext)
            else -> throw UnexpectedSchemaFormat(format, path, asyncApiContext)
        }
    }

    private fun isAsyncApiFormat(format: String): Boolean {
        return format == AsyncApiConstants.ASYNCAPI_V_3_0_0 ||
            format == AsyncApiConstants.ASYNCAPI_V_3_0_0_JSON ||
            format == AsyncApiConstants.ASYNCAPI_V_3_0_0_YAML
    }

    private fun isJsonSchemaDraftFormat(format: String): Boolean {
        return format == AsyncApiConstants.JSON_SCHEMA_DRAFT_07_JSON ||
            format == AsyncApiConstants.JSON_SCHEMA_DRAFT_07_YAML
    }

    private fun isAvroFormat(format: String): Boolean {
        return format == AsyncApiConstants.AVRO_V_1_9_0 ||
            format == AsyncApiConstants.AVRO_V_1_9_0_JSON ||
            format == AsyncApiConstants.AVRO_V_1_9_0_YAML
    }

    private fun isOpenApiFormat(format: String): Boolean {
        return format == AsyncApiConstants.OPENAPI_V_3_0_0 ||
            format == AsyncApiConstants.OPENAPI_V_3_0_0_JSON ||
            format == AsyncApiConstants.OPENAPI_V_3_0_0_YAML
    }

    private fun isRamlFormat(format: String): Boolean {
        return format == AsyncApiConstants.RAML_V_1_0_YAML
    }

    private fun isProtobufFormat(format: String): Boolean {
        return format == AsyncApiConstants.PROTOBUF_V_2 ||
            format == AsyncApiConstants.PROTOBUF_V_3
    }
}

