package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

data class AnalyzedMessage(
    val messageName: String, // The message name (e.g. "UserSignedUp")
    val payloadTypeName: String?, // The payload type name, or null when the Message Object has no payload
    val schema: Schema?, // The payload schema, or null when the Message Object has no payload
    val keySchema: SchemaInterface? = null,
    val headers: AnalyzedMessageHeaders? = null,
    val messageId: String = messageName,
) {
    val hasPayload: Boolean get() = schema != null
}
