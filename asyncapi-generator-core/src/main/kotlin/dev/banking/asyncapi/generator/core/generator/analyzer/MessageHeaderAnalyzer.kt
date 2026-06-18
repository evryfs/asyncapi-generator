package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Resolves generated header DTO metadata from AsyncAPI message headers.
 *
 * Expected behavior is covered by:
 * - `ChannelAnalyzerTest`
 * - `JavaModelPreparerTest`
 * - `KotlinModelPreparerTest`
 */
object MessageHeaderAnalyzer {
    fun analyze(
        channelName: String,
        messageKey: String,
        message: Message,
    ): AnalyzedMessageHeaders? {
        val properties = collectProperties(message)
        if (properties.isEmpty()) return null

        val channelNamePascal = MapperUtil.toPascalCase(channelName)
        val messageName = message.name ?: message.title ?: messageKey
        val messageNamePascal = MapperUtil.toPascalCase(messageName)

        return AnalyzedMessageHeaders(
            typeName = "Topic${channelNamePascal}Headers$messageNamePascal",
            properties = properties,
        )
    }

    private fun collectProperties(message: Message): Map<String, SchemaInterface> {
        val headers = mutableMapOf<String, SchemaInterface>()

        message.traits?.forEach { traitInterface ->
            val trait =
                when (traitInterface) {
                    is MessageTraitInterface.InlineMessageTrait -> traitInterface.trait
                    is MessageTraitInterface.ReferenceMessageTrait -> traitInterface.reference.model as? MessageTrait
                }
            trait?.headers?.let { headers.putAll(extractProperties(it)) }
        }

        message.headers?.let { headers.putAll(extractProperties(it)) }

        return headers
    }

    private fun extractProperties(schemaInterface: SchemaInterface): Map<String, SchemaInterface> =
        when (schemaInterface) {
            is SchemaInterface.SchemaInline ->
                if (schemaInterface.schema.type.getPrimaryType() == "object") {
                    schemaInterface.schema.properties ?: emptyMap()
                } else {
                    emptyMap()
                }
            is SchemaInterface.SchemaReference -> {
                val schema = schemaInterface.reference.model as? Schema
                if (schema?.type.getPrimaryType() == "object") {
                    schema?.properties ?: emptyMap()
                } else {
                    emptyMap()
                }
            }
            else -> emptyMap()
        }
}
