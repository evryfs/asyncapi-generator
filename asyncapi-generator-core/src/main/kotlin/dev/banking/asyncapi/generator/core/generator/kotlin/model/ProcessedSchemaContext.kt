package dev.banking.asyncapi.generator.core.generator.kotlin.model

import dev.banking.asyncapi.generator.core.model.schemas.Schema

data class ProcessedSchemaContext(
    val schemas: Map<String, Schema>,
    val polymorphicSchemas: Map<String, List<String>>
)
