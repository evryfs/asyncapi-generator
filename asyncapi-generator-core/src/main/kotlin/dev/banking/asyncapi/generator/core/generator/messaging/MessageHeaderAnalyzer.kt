package dev.banking.asyncapi.generator.core.generator.messaging

import dev.banking.asyncapi.generator.core.generator.normalizer.CompositionNormalizer
import dev.banking.asyncapi.generator.core.generator.schema.getPrimaryType
import dev.banking.asyncapi.generator.core.generator.schema.linkedSchema
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Collects the headers of a message, from its traits first and then from the message itself.
 *
 * Header schemas composed with `allOf` are flattened first. Only object header schemas contribute; a header
 * declared again replaces the earlier one.
 */
internal object MessageHeaderAnalyzer {
    private val compositionNormalizer = CompositionNormalizer()

    fun analyze(message: Message): AnalyzedMessageHeaders? {
        val headerSchemas =
            (
                message.traits.orEmpty().mapNotNull { trait -> trait.resolvedOrNull()?.headers } +
                    listOfNotNull(message.headers)
            ).mapNotNull(::objectSchema)
        val properties =
            headerSchemas.fold(linkedMapOf<String, SchemaInterface>()) { headers, schema ->
                headers.apply {
                    putAll(schema.properties.orEmpty())
                }
            }
        if (properties.isEmpty()) return null

        return AnalyzedMessageHeaders(
            properties = properties,
            requiredProperties = headerSchemas.flatMap { schema -> schema.required.orEmpty() }.distinct(),
        )
    }

    private fun objectSchema(headers: SchemaInterface): Schema? =
        when (headers) {
            is SchemaInterface.SchemaInline -> headers.schema
            is SchemaInterface.SchemaReference -> headers.linkedSchema()
            else -> null
        }?.let { schema -> compositionNormalizer.normalize(schema, "headers") }
            ?.takeIf { schema -> schema.type.getPrimaryType() == "object" }
}
