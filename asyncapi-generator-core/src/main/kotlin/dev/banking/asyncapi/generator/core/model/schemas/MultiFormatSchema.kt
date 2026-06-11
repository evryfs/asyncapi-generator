package dev.banking.asyncapi.generator.core.model.schemas

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Parsed representation of an AsyncAPI schema object that declares an explicit
 * non-AsyncAPI `schemaFormat`.
 *
 * The original [schemaFormat] string is retained for serialization and bundling,
 * while [format] gives later stages a typed way to distinguish native formats.
 *
 * Expected behavior is covered by:
 * - `MultiFormatSchemaParserTest`
 */
data class MultiFormatSchema(
    val schemaFormat: String,
    val schema: Any?,
    @get:JsonIgnore
    val format: SchemaFormat = requireNotNull(SchemaFormat.fromValue(schemaFormat)) {
        "Unknown schemaFormat '$schemaFormat'"
    },
)
