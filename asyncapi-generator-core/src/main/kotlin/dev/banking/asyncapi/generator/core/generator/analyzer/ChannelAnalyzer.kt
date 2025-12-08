package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
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
            val op = when (opInterface) {
                is OperationInterface.OperationInline -> opInterface.operation
                is OperationInterface.OperationReference -> opInterface.reference.model as? Operation
            } ?: return@forEach
            val channelRef = op.channel ?: return@forEach
            val targetChannelName = channels.entries.find { (_, chInterface) ->
                val ch =
                    if (chInterface is ChannelInterface.ChannelInline){
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

        val analyzedChannels = channels.mapNotNull { (name, chInterface) ->
            val channel = when (chInterface) {
                is ChannelInterface.ChannelInline -> chInterface.channel
                is ChannelInterface.ChannelReference -> chInterface.reference.model as? Channel
            } ?: return@mapNotNull null

            val usage = channelUsage[name]!!
            val finalProducer = if (!usage.isProducer && !usage.isConsumer) true else usage.isProducer
            val finalConsumer = if (!usage.isProducer && !usage.isConsumer) true else usage.isConsumer

            AnalyzedChannel(
                channelName = name,
                topic = channel.address ?: name, // Fallback if address missing
                isProducer = finalProducer,
                isConsumer = finalConsumer,
                messages = resolveMessages(channel.messages)
            )
        }

        return ChannelAnalysisResult(analyzedChannels)
    }

    private fun resolveMessages(messages: Map<String, MessageInterface>?): List<AnalyzedMessage> {
        if (messages.isNullOrEmpty()) return emptyList()
        return messages.mapNotNull { (name, msgInterface) ->
            val message = when (msgInterface) {
                is MessageInterface.MessageInline -> msgInterface.message
                is MessageInterface.MessageReference -> msgInterface.reference.model as? Message
            } ?: return@mapNotNull null

            var payloadSchema: Schema? = null
            var typeName: String? = null

            when (val p = message.payload) {
                is SchemaInterface.SchemaInline -> {
                    payloadSchema = p.schema
                    typeName = MapperUtil.toPascalCase(message.name ?: message.title ?: name)
                }
                is SchemaInterface.SchemaReference -> {
                    payloadSchema = p.reference.model as? Schema
                    typeName = MapperUtil.toPascalCase(p.reference.ref.substringAfterLast('/'))
                }
                else -> {}
            }

            if (payloadSchema == null || typeName == null) return@mapNotNull null

            AnalyzedMessage(
                name = typeName,
                schema = payloadSchema
            )
        }
    }
}
