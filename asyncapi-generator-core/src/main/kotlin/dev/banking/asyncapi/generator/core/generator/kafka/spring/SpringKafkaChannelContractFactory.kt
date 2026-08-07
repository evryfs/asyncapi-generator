package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType
import dev.banking.asyncapi.generator.core.generator.configuration.TopicParameterProperties
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil

/** Prepares the language-neutral channel and method contract rendered by Spring Kafka generators. */
internal class SpringKafkaChannelContractFactory(
    private val modelPackage: String,
    private val additionalPayloadTypes: Set<AdditionalProducerPayloadType>,
    private val topicParameterProperties: TopicParameterProperties,
    nativeKafkaPayloadResolver: NativeKafkaPayloadResolver = NativeKafkaPayloadResolver(),
) {
    private val payloadFactory = KafkaPayloadFactory(modelPackage, nativeKafkaPayloadResolver)

    fun create(channel: AnalyzedChannel): SpringKafkaChannelContract {
        val messages =
            payloadFactory.create(channel).map { payload ->
                SpringKafkaMessageContract(
                    payload = payload,
                    keyContract =
                        KafkaKeyContractResolver.resolve(
                            messageName = payload.messageName,
                            schema = payload.keySchema,
                            modelPackage = modelPackage,
                        ),
                    consumerMethodName = "listen${payload.messageName}",
                    producerMethods =
                        producerPayloadMethods(
                            messageName = payload.messageName,
                            hasPayload = payload.hasPayload,
                            additionalPayloadTypes = additionalPayloadTypes,
                        ),
                )
            }

        return SpringKafkaChannelContract(
            baseName = MapperUtil.toPascalCase(channel.channelName),
            topic = channel.topic,
            topicAddress =
                KafkaTopicAddress.from(
                    channelName = channel.channelName,
                    value = channel.topic,
                    topicParameterProperties = topicParameterProperties,
                ),
            messages = messages,
        )
    }
}

internal data class SpringKafkaChannelContract(
    val baseName: String,
    val topic: String,
    val topicAddress: KafkaTopicAddress,
    val messages: List<SpringKafkaMessageContract>,
)

internal data class SpringKafkaMessageContract(
    val payload: KafkaPayload,
    val keyContract: KafkaKeyContract?,
    val consumerMethodName: String,
    val producerMethods: List<ProducerPayloadMethod>,
)
