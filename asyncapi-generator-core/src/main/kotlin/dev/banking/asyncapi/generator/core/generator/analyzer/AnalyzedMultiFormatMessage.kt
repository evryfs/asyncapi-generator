package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema

/**
 * Message payload backed by an explicit non-AsyncAPI `schemaFormat`.
 *
 * These payloads are kept separate from [AnalyzedMessage] because they should not
 * be normalized or generated as JSON-compatible model schemas.
 *
 * Expected behavior is covered by:
 * - `ChannelAnalyzerTest`
 */
data class AnalyzedMultiFormatMessage(
    val messageName: String,
    val payloadName: String,
    val schema: MultiFormatSchema,
    val headers: AnalyzedMessageHeaders? = null,
)
