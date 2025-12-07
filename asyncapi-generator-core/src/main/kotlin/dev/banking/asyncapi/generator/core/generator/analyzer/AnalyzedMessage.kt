package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.model.schemas.Schema

data class AnalyzedMessage(
    val name: String, // The class name (e.g. "UserSignedUp")
    val schema: Schema, // The payload schema
    val keySchema: Schema? = null // Optional Kafka Key schema
)
