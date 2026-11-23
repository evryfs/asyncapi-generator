package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Typed Spring Kafka client generation requested by generator configuration.
 */
data class ClientGeneration(
    val packageName: String,
    val modelPackageName: String,
    val clientContract: ClientContract = ClientContract.INTERFACE,
    val topicParameterProperties: TopicParameterProperties = TopicParameterProperties.EMPTY,
    val validationAnnotations: ClientValidationAnnotations = ClientValidationAnnotations(),
    val producer: Producer = Producer(),
    val consumer: Consumer = Consumer(),
) {
    init {
        require(producer.enabled || consumer.enabled) {
            "Spring Kafka client generation requires at least one enabled contract: " +
                "producer.enabled or consumer.enabled"
        }
    }

    data class Producer(
        val enabled: Boolean = true,
        val additionalPayloadTypes: Set<AdditionalProducerPayloadType> = emptySet(),
    )

    data class Consumer(
        val enabled: Boolean = true,
    )
}
