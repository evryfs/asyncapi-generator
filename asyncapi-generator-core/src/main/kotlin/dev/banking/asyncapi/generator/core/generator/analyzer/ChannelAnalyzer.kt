package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class ChannelAnalyzer {
    private data class ChannelUsage(
        var isProducer: Boolean = false,
        var isConsumer: Boolean = false,
    )

    fun analyze(document: AsyncApiDocument): ChannelAnalysisResult {
        val channels = document.channels ?: return ChannelAnalysisResult(emptyList())
        val operations = document.operations ?: emptyMap()

        val channelUsage = mutableMapOf<String, ChannelUsage>()
        channels.keys.forEach { name -> channelUsage[name] = ChannelUsage() }

        operations.values.forEach { opInterface ->
            val op =
                when (opInterface) {
                    is OperationInterface.OperationInline -> opInterface.operation
                    is OperationInterface.OperationReference -> opInterface.reference.model as? Operation
                } ?: return@forEach
            val channelRef = op.channel ?: return@forEach
            val targetChannelName =
                channels.entries
                    .find { (_, chInterface) ->
                        val ch =
                            if (chInterface is ChannelInterface.ChannelInline) {
                                chInterface.channel
                            } else {
                                (chInterface as ChannelInterface.ChannelReference).reference.model
                            }
                        ch === channelRef.model
                    }?.key ?: return@forEach

            val usage = channelUsage[targetChannelName]!!
            if (op.action == "send") {
                usage.isProducer = true
            } else if (op.action == "receive") {
                usage.isConsumer = true
            }
        }

        val analyzedChannels =
            channels.mapNotNull { (name, chInterface) ->
                val channel =
                    when (chInterface) {
                        is ChannelInterface.ChannelInline -> chInterface.channel
                        is ChannelInterface.ChannelReference -> chInterface.reference.model as? Channel
                    } ?: return@mapNotNull null

                val usage = channelUsage[name]!!
                val finalProducer = if (!usage.isProducer && !usage.isConsumer) true else usage.isProducer
                val finalConsumer = if (!usage.isProducer && !usage.isConsumer) true else usage.isConsumer
                val resolvedMessages = resolveMessages(channelName = name, messages = channel.messages)

                AnalyzedChannel(
                    channelName = name,
                    topic = channel.address ?: name, // Fallback if address missing
                    isProducer = finalProducer,
                    isConsumer = finalConsumer,
                    messages = resolvedMessages.messages,
                    multiFormatMessages = resolvedMessages.multiFormatMessages,
                )
            }

        return ChannelAnalysisResult(analyzedChannels)
    }

    private fun resolveMessages(
        channelName: String,
        messages: Map<String, MessageInterface>?,
    ): ResolvedMessages {
        if (messages.isNullOrEmpty()) return ResolvedMessages()
        val analyzedMessages = mutableListOf<AnalyzedMessage>()
        val analyzedMultiFormatMessages = mutableListOf<AnalyzedMultiFormatMessage>()

        messages.forEach { (name, msgInterface) ->
            val message =
                when (msgInterface) {
                    is MessageInterface.MessageInline -> msgInterface.message
                    is MessageInterface.MessageReference -> msgInterface.reference.model as? Message
                } ?: return@forEach

            var payloadSchema: Schema? = null
            var multiFormatSchema: MultiFormatSchema? = null
            var typeName: String? = null
            val baseName = MapperUtil.toPascalCase(message.name ?: message.title ?: name)
            val inlinePayloadTypeName = if (baseName.endsWith("Payload")) baseName else "${baseName}Payload"
            val headers =
                MessageHeaderAnalyzer.analyze(
                    channelName = channelName,
                    messageKey = name,
                    message = message,
                )

            when (val p = message.payload) {
                is SchemaInterface.SchemaInline -> {
                    payloadSchema = p.schema
                    typeName = inlinePayloadTypeName
                }
                is SchemaInterface.SchemaReference -> {
                    typeName = MapperUtil.toPascalCase(p.reference.ref.substringAfterLast('/'))
                    when (val referencedModel = p.reference.model) {
                        is Schema -> payloadSchema = referencedModel
                        is MultiFormatSchema -> multiFormatSchema = referencedModel
                    }
                }
                is SchemaInterface.MultiFormatSchemaInline -> {
                    multiFormatSchema = p.multiFormatSchema
                    typeName = inlinePayloadTypeName
                }
                else -> {}
            }

            if (typeName == null) return@forEach

            if (payloadSchema != null) {
                analyzedMessages.add(
                    AnalyzedMessage(
                        messageName = baseName,
                        payloadTypeName = typeName,
                        schema = payloadSchema,
                        headers = headers,
                    ),
                )
            } else if (multiFormatSchema != null) {
                analyzedMultiFormatMessages.add(
                    AnalyzedMultiFormatMessage(
                        messageName = baseName,
                        payloadName = typeName,
                        schema = multiFormatSchema,
                        headers = headers,
                    ),
                )
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
