package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

data class AnalyzedMessageHeaders(
    val typeName: String,
    val properties: Map<String, SchemaInterface>,
    val requiredProperties: List<String> = emptyList(),
)
