package dev.banking.asyncapi.generator.core.generator.schema

import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema

/**
 * Retains supported contract schema declarations for artifact generation.
 *
 * @property asyncApiSchemas AsyncAPI Schema Object declarations by generated name
 * @property multiFormatSchemas Multi Format Schema Object declarations by generated name
 * @property booleanSchemas Boolean schema declarations by generated name
 */
data class SchemaDeclarationCatalog(
    val asyncApiSchemas: Map<String, Schema> = emptyMap(),
    val multiFormatSchemas: Map<String, MultiFormatSchema> = emptyMap(),
    val booleanSchemas: Map<String, Boolean> = emptyMap(),
)
