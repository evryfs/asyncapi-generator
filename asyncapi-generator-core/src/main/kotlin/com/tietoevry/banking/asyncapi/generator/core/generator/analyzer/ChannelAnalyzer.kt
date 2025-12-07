package com.tietoevry.banking.asyncapi.generator.core.generator.analyzer

import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil
import com.tietoevry.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import com.tietoevry.banking.asyncapi.generator.core.model.channels.Channel
import com.tietoevry.banking.asyncapi.generator.core.model.channels.ChannelInterface
import com.tietoevry.banking.asyncapi.generator.core.model.messages.Message
import com.tietoevry.banking.asyncapi.generator.core.model.messages.MessageInterface
import com.tietoevry.banking.asyncapi.generator.core.model.operations.Operation
import com.tietoevry.banking.asyncapi.generator.core.model.operations.OperationInterface
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class ChannelAnalyzer(private val context: AsyncApiContext) {

    private data class ChannelUsage(
        var isProducer: Boolean = false,
        var isConsumer: Boolean = false,
    )

    fun analyze(document: AsyncApiDocument): ChannelAnalysisResult {
        val channels = document.channels ?: return ChannelAnalysisResult(emptyList())
        val operations = document.operations ?: emptyMap()

        // 1. Map Channel Name -> Usage Flags
        val channelUsage = mutableMapOf<String, ChannelUsage>()
        channels.keys.forEach { name -> channelUsage[name] = ChannelUsage() }

        // 2. Scan Operations to determine usage
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
                // "send" means the Application sends -> Client is PRODUCER
                usage.isProducer = true
            } else if (op.action == "receive") {
                // "receive" means Application receives -> Client is CONSUMER
                usage.isConsumer = true
            }
        }

        // 3. Build Analyzed Channels
        val analyzedChannels = channels.mapNotNull { (name, chInterface) ->
            val channel = when (chInterface) {
                is ChannelInterface.ChannelInline -> chInterface.channel
                is ChannelInterface.ChannelReference -> chInterface.reference.model as? Channel
            } ?: return@mapNotNull null

            val usage = channelUsage[name]!!
            // If no operations claimed it, default to BOTH
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
                    // For inline schema, use Message Name (or Title) as class name
                    typeName = MapperUtil.toPascalCase(message.name ?: message.title ?: name)
                }
                is SchemaInterface.SchemaReference -> {
                    payloadSchema = p.reference.model as? Schema
                    // For reference, use the schema name from the reference path
                    // e.g. #/components/schemas/CustomerEmailPayload -> CustomerEmailPayload
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
