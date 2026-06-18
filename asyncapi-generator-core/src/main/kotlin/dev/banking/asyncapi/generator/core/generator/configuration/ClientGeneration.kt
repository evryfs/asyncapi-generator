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
        val headers: Headers = Headers(),
        val springKafka: SpringKafka? = null,
    ) : ClientGeneration

    data class Headers(
        val enabled: Boolean = true,
    )

    data class SpringKafka(
        val producer: Producer = Producer(),
        val consumer: Consumer = Consumer(),
    )

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
