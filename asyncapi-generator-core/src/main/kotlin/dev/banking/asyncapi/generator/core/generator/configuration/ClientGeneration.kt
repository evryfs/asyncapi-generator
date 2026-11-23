package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Typed Spring Kafka client generation requested by generator configuration.
 *
 * @param packageName the package of the generated client contracts.
 * @param modelPackageName the package of the payload models the contracts use.
 * @param clientContract the shape of the generated contracts.
 * @param topicParameterProperties Spring property names for parameters in Kafka topic addresses.
 * @param validationAnnotations annotations placed on the contracts and their payload parameters.
 * @param producer the producer contract settings.
 * @param consumer the consumer contract settings.
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
                "clientConfig.producer.enabled or clientConfig.consumer.enabled"
        }
    }

    /**
     * Producer contract settings.
     *
     * @param enabled whether producer contracts are generated.
     * @param additionalPayloadTypes serialized payload types that get their own producer method.
     */
    data class Producer(
        val enabled: Boolean = true,
        val additionalPayloadTypes: Set<AdditionalProducerPayloadType> = emptySet(),
    )

    /**
     * Consumer contract settings.
     *
     * @param enabled whether consumer contracts are generated.
     */
    data class Consumer(
        val enabled: Boolean = true,
    )
}
