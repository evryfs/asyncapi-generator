package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.SpringKafkaClientChannelWithoutAddress
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.SpringKafkaClientChannelWithoutMessages
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.SpringKafkaClientContractNameCollision

/** Validates source-contract invariants required by generated Spring Kafka clients. */
internal object SpringKafkaClientContractValidator {
    fun validate(
        channels: List<AnalyzedChannel>,
        task: GenerationTask.SpringKafkaClient,
    ) {
        if (!task.generateProducers && !task.generateConsumers) {
            return
        }

        channels
            .firstOrNull { channel -> channel.topic == null }
            ?.let { channel ->
                throw SpringKafkaClientChannelWithoutAddress(channel.channelName)
            }

        channels
            .firstOrNull { channel ->
                channel.messages.isEmpty() && channel.multiFormatMessages.isEmpty()
            }?.let { channel ->
                throw SpringKafkaClientChannelWithoutMessages(channel.channelName)
            }

        val collision =
            channels
                .groupBy { channel -> MapperUtil.toPascalCase(channel.channelName) }
                .asSequence()
                .filter { (_, matchingChannels) -> matchingChannels.size > 1 }
                .sortedBy { (generatedBaseName, _) -> generatedBaseName }
                .firstOrNull()

        if (collision != null) {
            val generatedBaseName = collision.key
            throw SpringKafkaClientContractNameCollision(
                channelNames = collision.value.map(AnalyzedChannel::channelName).sorted(),
                generatedBaseName = generatedBaseName,
                contractNames =
                    buildList {
                        if (task.generateProducers) add("${generatedBaseName}Producer")
                        if (task.generateConsumers) add("${generatedBaseName}Consumer")
                    },
            )
        }

        SpringKafkaClientMethodNameValidator.validate(
            channels = channels,
            task = task,
        )
    }
}
