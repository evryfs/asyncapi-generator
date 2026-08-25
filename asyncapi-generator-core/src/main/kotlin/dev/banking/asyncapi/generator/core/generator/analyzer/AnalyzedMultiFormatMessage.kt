package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Message payload backed by an explicit non-AsyncAPI `schemaFormat`.
 *
 * These payloads are kept separate from [AnalyzedMessage] because they should not
 * be normalized or generated as JSON-compatible model schemas.
 */
data class AnalyzedMultiFormatMessage(
    val messageName: String,
    val payloadName: String,
    val schema: MultiFormatSchema,
    val keySchema: SchemaInterface? = null,
    val headers: AnalyzedMessageHeaders? = null,
    val messageId: String = messageName,
)
