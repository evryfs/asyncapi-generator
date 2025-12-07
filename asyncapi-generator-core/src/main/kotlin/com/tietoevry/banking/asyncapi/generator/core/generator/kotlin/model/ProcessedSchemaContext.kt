package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.model

import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema

data class ProcessedSchemaContext(
    val schemas: Map<String, Schema>,
    val polymorphicSchemas: Map<String, List<String>>
)
