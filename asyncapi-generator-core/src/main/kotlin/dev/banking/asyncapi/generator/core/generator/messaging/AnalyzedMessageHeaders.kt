package dev.banking.asyncapi.generator.core.generator.messaging

import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * The headers of a message.
 *
 * @param properties Header schemas by wire name.
 * @param requiredProperties Wire names of the required headers.
 */
internal data class AnalyzedMessageHeaders(
    val properties: Map<String, SchemaInterface>,
    val requiredProperties: List<String> = emptyList(),
)
