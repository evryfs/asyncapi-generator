package dev.banking.asyncapi.generator.core.generator.loader

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

object HeaderSchemaCollector {
    fun collect(asyncApiDocument: AsyncApiDocument): Map<String, Schema> {
        val channels = asyncApiDocument.channels ?: return emptyMap()
        val headerSchemas = mutableMapOf<String, Schema>()
        channels.forEach { (channelName, channelInterface) ->
            val channel =
                when (channelInterface) {
                    is ChannelInterface.ChannelInline -> channelInterface.channel
                    is ChannelInterface.ChannelReference -> channelInterface.reference.model as? Channel
                } ?: return@forEach
            channel.messages?.forEach { (messageKey, messageInterface) ->
                val message =
                    when (messageInterface) {
                        is MessageInterface.MessageInline -> messageInterface.message
                        is MessageInterface.MessageReference -> messageInterface.reference.model as? Message
                    } ?: return@forEach
                val headers = collectHeaders(message)
                if (headers.isEmpty()) return@forEach
                val channelNamePascal = MapperUtil.toPascalCase(channelName)
                val messageName = message.name ?: message.title ?: messageKey
                val messageNamePascal = MapperUtil.toPascalCase(messageName)
                val schemaName = "Topic${channelNamePascal}Headers$messageNamePascal"
                headerSchemas[schemaName] =
                    Schema(
                        type = "object",
                        properties = headers,
                    )
            }
        }
        return headerSchemas
    }

    private fun collectHeaders(message: Message): Map<String, SchemaInterface> {
        val headers = mutableMapOf<String, SchemaInterface>()

        message.traits?.forEach { traitInterface ->
            val trait =
                when (traitInterface) {
                    is MessageTraitInterface.InlineMessageTrait -> traitInterface.trait
                    is MessageTraitInterface.ReferenceMessageTrait -> traitInterface.reference.model as? MessageTrait
                }
            trait?.headers?.let { headers.putAll(extractHeaderProperties(it)) }
        }

        message.headers?.let { headers.putAll(extractHeaderProperties(it)) }

        return headers
    }

    private fun extractHeaderProperties(schemaInterface: SchemaInterface): Map<String, SchemaInterface> =
        when (schemaInterface) {
            is SchemaInterface.SchemaInline -> {
                val schema = schemaInterface.schema
                if (schema.type.getPrimaryType() == "object") schema.properties ?: emptyMap() else emptyMap()
            }
            is SchemaInterface.SchemaReference -> {
                val schema = schemaInterface.reference.model as? Schema
                if (schema?.type.getPrimaryType() == "object") schema?.properties ?: emptyMap() else emptyMap()
            }
            else -> emptyMap()
        }
}
