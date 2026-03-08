package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.model.schemas.Schema

data class AnalyzedMessage(
    val messageName: String, // The message name (e.g. "UserSignedUp")
    val payloadTypeName: String, // The payload type name (e.g. "UserSignedUpPayload")
    val schema: Schema, // The payload schema
    val keySchema: Schema? = null, // Optional Kafka Key schema
)
