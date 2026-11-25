package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType
import dev.banking.asyncapi.generator.core.generator.configuration.TopicParameterProperties
import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.sourcemodel.NameCase

/**
 * Prepares the language-neutral channel and method contract rendered by Spring Kafka generators.
 *
 * @param additionalPayloadTypes Serialized payload types producers also send.
 * @param topicParameterProperties Spring properties that supply channel address parameters.
 * @param nativeKafkaPayloadResolver Resolves payloads in native schema formats.
 */
internal class SpringKafkaChannelContractFactory(
    private val additionalPayloadTypes: Set<AdditionalProducerPayloadType>,
    private val topicParameterProperties: TopicParameterProperties,
    nativeKafkaPayloadResolver: NativeKafkaPayloadResolver = NativeKafkaPayloadResolver(),
) {
    private val payloadFactory = KafkaPayloadFactory(nativeKafkaPayloadResolver)

    fun create(channel: AnalyzedChannel): SpringKafkaChannelContract {
        val topic =
            checkNotNull(channel.topic) {
                "Spring Kafka channel '${channel.channelName}' must have an address after compatibility validation."
            }
        return SpringKafkaChannelContract(
            baseName = NameCase.toPascalCase(channel.channelName),
            topic = topic,
            topicAddress =
                KafkaTopicAddress.from(
                    channelName = channel.channelName,
                    value = topic,
                    topicParameterProperties = topicParameterProperties,
                ),
            messages = payloadFactory.create(channel).map(::messageContract),
        )
    }

    private fun messageContract(payload: KafkaPayload): SpringKafkaMessageContract =
        SpringKafkaMessageContract(
            payload = payload,
            keyContract = KafkaKeyContractResolver.resolve(payload.kafkaKey),
            consumerMethodName = "listen${payload.messageName}",
            producerMethods =
                producerPayloadMethods(
                    messageName = payload.messageName,
                    hasPayload = payload.hasPayload,
                    additionalPayloadTypes = additionalPayloadTypes,
                ),
        )
}

/**
 * The client contract of one channel.
 *
 * @param baseName Name the channel's consumer and producer are named after.
 * @param topic Address of the channel.
 * @param topicAddress The address as a Spring property placeholder and its constant name.
 * @param messages Contracts of the channel's messages.
 */
internal data class SpringKafkaChannelContract(
    val baseName: String,
    val topic: String,
    val topicAddress: KafkaTopicAddress,
    val messages: List<SpringKafkaMessageContract>,
)

/**
 * The client contract of one message.
 *
 * @param payload Payload, key and header signature of the message.
 * @param keyContract Record key parameter, or `null` when the message declares no key.
 * @param consumerMethodName Name of the listener method.
 * @param producerMethods Send methods, one for the contract payload and one per additional payload type.
 */
internal data class SpringKafkaMessageContract(
    val payload: KafkaPayload,
    val keyContract: KafkaKeyContract?,
    val consumerMethodName: String,
    val producerMethods: List<ProducerPayloadMethod>,
)
