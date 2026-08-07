package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.SpringKafkaClientMethodNameCollision

/** Rejects message names that would produce duplicate methods in one generated Spring Kafka client contract. */
internal object SpringKafkaClientMethodNameValidator {
    fun validate(
        channels: List<AnalyzedChannel>,
        task: GenerationTask.SpringKafkaClient,
    ) {
        channels.forEach { channel ->
            val collision =
                channel.messageIdentities()
                    .flatMap { message -> message.generatedMethods(task) }
                    .groupBy { method -> method.scope to method.name }
                    .asSequence()
                    .filter { (_, methods) -> methods.size > 1 }
                    .sortedWith(
                        compareBy(
                            { (method, _) -> method.first.ordinal },
                            { (method, _) -> method.second },
                        ),
                    )
                    .firstOrNull()
                    ?: return@forEach

            throw SpringKafkaClientMethodNameCollision(
                channelName = channel.channelName,
                messageIds = collision.value.map(GeneratedMethod::messageId).sorted(),
                methodName = collision.key.second,
            )
        }
    }

    private fun MessageIdentity.generatedMethods(
        task: GenerationTask.SpringKafkaClient,
    ): List<GeneratedMethod> =
        buildList {
            if (task.generateProducers) {
                producerPayloadMethods(
                    messageName = generatedName,
                    hasPayload = hasPayload,
                    additionalPayloadTypes = task.additionalPayloadTypes,
                ).forEach { producerMethod ->
                    add(
                        GeneratedMethod(
                            messageId = messageId,
                            name = producerMethod.methodName,
                            scope = MethodScope.PRODUCER,
                        ),
                    )
                }
            }
            if (task.generateConsumers) {
                add(
                    GeneratedMethod(
                        messageId = messageId,
                        name = "listen$generatedName",
                        scope = MethodScope.CONSUMER,
                    ),
                )
            }
        }

    private fun AnalyzedChannel.messageIdentities(): List<MessageIdentity> =
        messages.map { message ->
            MessageIdentity(
                messageId = message.messageId,
                generatedName = message.messageName,
                hasPayload = message.schema != null,
            )
        } +
            multiFormatMessages
                .filter { message ->
                    message.schema.format.isNativeAvro || message.schema.format.isNativeProtobuf
                }
                .map { message ->
                    MessageIdentity(
                        messageId = message.messageId,
                        generatedName = message.messageName,
                        hasPayload = true,
                    )
                }

    private data class MessageIdentity(
        val messageId: String,
        val generatedName: String,
        val hasPayload: Boolean,
    )

    private data class GeneratedMethod(
        val messageId: String,
        val name: String,
        val scope: MethodScope,
    )

    private enum class MethodScope {
        PRODUCER,
        CONSUMER,
    }
}
