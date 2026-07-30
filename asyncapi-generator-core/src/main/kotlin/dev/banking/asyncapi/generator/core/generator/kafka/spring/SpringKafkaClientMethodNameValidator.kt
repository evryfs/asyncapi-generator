package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.SpringKafkaClientMethodNameCollision

/**
 * Rejects message names that would produce duplicate methods in one generated
 * Spring Kafka client contract.
 *
 * Expected behavior is covered by `SpringKafkaClientMethodNameValidatorTest`.
 */
internal object SpringKafkaClientMethodNameValidator {
    fun validate(
        channels: List<AnalyzedChannel>,
        task: GenerationTask.SpringKafkaClient,
    ) {
        channels.forEach { channel ->
            val methodPrefixes = channel.generatedMethodPrefixes(task)
            if (methodPrefixes.isEmpty()) return@forEach

            val collision =
                channel.messageIdentities()
                    .groupBy(MessageIdentity::generatedName)
                    .asSequence()
                    .filter { (_, messages) -> messages.size > 1 }
                    .sortedBy { (generatedName, _) -> generatedName }
                    .firstOrNull()
                    ?: return@forEach

            val generatedMessageName = collision.key
            throw SpringKafkaClientMethodNameCollision(
                channelName = channel.channelName,
                messageIds = collision.value.map(MessageIdentity::messageId).sorted(),
                generatedMessageName = generatedMessageName,
                methodNames = methodPrefixes.map { prefix -> "$prefix$generatedMessageName" },
            )
        }
    }

    private fun AnalyzedChannel.generatedMethodPrefixes(
        task: GenerationTask.SpringKafkaClient,
    ): List<String> =
        buildList {
            if (task.generateProducers) add("send")
            if (task.generateConsumers) add("listen")
        }

    private fun AnalyzedChannel.messageIdentities(): List<MessageIdentity> =
        messages.map { message ->
            MessageIdentity(
                messageId = message.messageId,
                generatedName = message.messageName,
            )
        } +
            multiFormatMessages.map { message ->
                MessageIdentity(
                    messageId = message.messageId,
                    generatedName = message.messageName,
                )
            }

    private data class MessageIdentity(
        val messageId: String,
        val generatedName: String,
    )
}
