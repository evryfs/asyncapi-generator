package dev.banking.asyncapi.generator.core.generator.loader

import dev.banking.asyncapi.generator.core.generator.analyzer.MessageHeaderAnalyzer
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.schemas.Schema

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
                val headers =
                    MessageHeaderAnalyzer.analyze(
                        channelName = channelName,
                        messageKey = messageKey,
                        message = message,
                    ) ?: return@forEach
                headerSchemas[headers.typeName] =
                    Schema(
                        type = "object",
                        properties = headers.properties,
                    )
            }
        }
        return headerSchemas
    }
}
