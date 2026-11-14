package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.model.schemas.Schema

/** Schema models and relationships prepared for source generation. */
internal data class SchemaAnalysis(
    val schemas: Map<String, Schema>,
    val polymorphicRelationships: Map<String, List<String>>,
)
