package dev.banking.asyncapi.generator.core.generator.loader

import dev.banking.asyncapi.generator.core.generator.schema.SchemaDeclarationCatalog
import dev.banking.asyncapi.generator.core.model.schemas.Schema

/**
 * Schema inputs collected from an AsyncAPI document for generation.
 *
 * @property schemas source-model schemas
 * @property schemaDeclarations supported contract schema declarations
 */
data class LoadedSchemas(
    val schemas: Map<String, Schema>,
    val schemaDeclarations: SchemaDeclarationCatalog,
)
