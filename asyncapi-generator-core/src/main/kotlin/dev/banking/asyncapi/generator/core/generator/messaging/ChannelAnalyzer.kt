package dev.banking.asyncapi.generator.core.generator.messaging

import dev.banking.asyncapi.generator.core.generator.messaging.kafka.KafkaKey
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface

/**
 * Reads the channels of a document and the messages each one carries.
 *
 * Messages with a boolean payload schema, or a payload reference that does not resolve to a schema, are left out.
 */
internal class ChannelAnalyzer {
    fun analyze(document: AsyncApiDocument): List<AnalyzedChannel> =
        document.channels.orEmpty().mapNotNull { (channelName, channelInterface) ->
            val channel = channelInterface.resolvedOrNull() ?: return@mapNotNull null
            AnalyzedChannel(
                channelName = channelName,
                topic = channel.address,
                messages = analyzeMessages(channel.messages.orEmpty()),
            )
        }

    private fun analyzeMessages(messages: Map<String, MessageInterface>): List<AnalyzedMessage> {
        val analyzed =
            messages.mapNotNull { (messageId, messageInterface) ->
                analyzeMessage(messageId, messageInterface)
            }
        val (nativeMessages, otherMessages) = analyzed.partition { message -> message.nativePayload != null }
        return otherMessages + nativeMessages
    }

    private fun analyzeMessage(
        messageId: String,
        messageInterface: MessageInterface,
    ): AnalyzedMessage? {
        val message = MessagePayloadResolver.resolveMessage(messageInterface) ?: return null
        val payload = MessagePayloadResolver.resolvePayload(message, messageId) as? MessagePayload
        val messageName = MessageNameResolver.resolve(message, messageId)
        return AnalyzedMessage(
            messageId = messageId,
            messageName = messageName,
            payload = payload,
            kafkaKey = KafkaKey.of(messageName, message),
            headers = MessageHeaderAnalyzer.analyze(message),
        ).takeUnless { payload == null && message.payload != null }
    }
}
