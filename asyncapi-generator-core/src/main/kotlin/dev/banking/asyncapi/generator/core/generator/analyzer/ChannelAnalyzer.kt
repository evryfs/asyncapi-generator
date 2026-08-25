package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.generator.kafka.kafkaKeySchema
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface

class ChannelAnalyzer {
    fun analyze(document: AsyncApiDocument): List<AnalyzedChannel> {
        val channels = document.channels ?: return emptyList()

        val analyzedChannels =
            channels.mapNotNull { (name, chInterface) ->
                val channel =
                    when (chInterface) {
                        is ChannelInterface.ChannelInline -> chInterface.channel
                        is ChannelInterface.ChannelReference -> chInterface.reference.model as? Channel
                    } ?: return@mapNotNull null

                val resolvedMessages = resolveMessages(channelName = name, messages = channel.messages)

                AnalyzedChannel(
                    channelName = name,
                    topic = channel.address,
                    messages = resolvedMessages.messages,
                    multiFormatMessages = resolvedMessages.multiFormatMessages,
                )
            }

        return analyzedChannels
    }

    private fun resolveMessages(
        channelName: String,
        messages: Map<String, MessageInterface>?,
    ): ResolvedMessages {
        if (messages.isNullOrEmpty()) return ResolvedMessages()
        val analyzedMessages = mutableListOf<AnalyzedMessage>()
        val analyzedMultiFormatMessages = mutableListOf<AnalyzedMultiFormatMessage>()

        messages.forEach { (messageId, msgInterface) ->
            val message = MessagePayloadResolver.resolveMessage(msgInterface) ?: return@forEach
            val messageName = MessageNameResolver.resolve(message, messageId)
            val headers =
                MessageHeaderAnalyzer.analyze(
                    message = message,
                )
            val keySchema = message.kafkaKeySchema()

            when (val payload = MessagePayloadResolver.resolvePayload(message, messageId)) {
                is ResolvedMessagePayload.AsyncApi ->
                    analyzedMessages.add(
                        AnalyzedMessage(
                            messageName = messageName,
                            payloadTypeName = payload.typeName,
                            schema = payload.schema,
                            keySchema = keySchema,
                            headers = headers,
                            messageId = messageId,
                        ),
                    )
                is ResolvedMessagePayload.MultiFormat ->
                    analyzedMultiFormatMessages.add(
                        AnalyzedMultiFormatMessage(
                            messageName = messageName,
                            payloadName = payload.typeName,
                            schema = payload.schema,
                            keySchema = keySchema,
                            headers = headers,
                            messageId = messageId,
                        ),
                    )
                is ResolvedMessagePayload.Boolean -> Unit
                null ->
                    if (message.payload == null) {
                        analyzedMessages.add(
                            AnalyzedMessage(
                                messageName = messageName,
                                payloadTypeName = null,
                                schema = null,
                                keySchema = keySchema,
                                headers = headers,
                                messageId = messageId,
                            ),
                        )
                    }
            }
        }

        return ResolvedMessages(
            messages = analyzedMessages,
            multiFormatMessages = analyzedMultiFormatMessages,
        )
    }

    private data class ResolvedMessages(
        val messages: List<AnalyzedMessage> = emptyList(),
        val multiFormatMessages: List<AnalyzedMultiFormatMessage> = emptyList(),
    )
}
