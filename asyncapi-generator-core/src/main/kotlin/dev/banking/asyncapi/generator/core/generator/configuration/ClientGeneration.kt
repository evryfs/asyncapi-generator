package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Typed client generation capabilities requested by generator configuration.
 *
 * Expected behavior is covered by:
 * - `GenerationPlannerTest`
 */
sealed interface ClientGeneration {
    data class Kafka(
        val packageName: String,
        val modelPackageName: String,
        val springKafka: SpringKafka? = null,
    ) : ClientGeneration

    data class SpringKafka(
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
    }

    data class Producer(
        val enabled: Boolean = true,
    )

    data class Consumer(
        val enabled: Boolean = true,
    )

    data class QuarkusKafka(
        val packageName: String,
        val modelPackageName: String,
    ) : ClientGeneration
}
