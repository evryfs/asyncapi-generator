package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Typed client generation capabilities requested by generator configuration.
 *
 * Expected behavior is covered by:
 * - `GenerationPlannerTest`
 */
sealed interface ClientGeneration {
    data class SpringKafka(
        val packageName: String,
        val modelPackageName: String,
    ) : ClientGeneration

    data class QuarkusKafka(
        val packageName: String,
        val modelPackageName: String,
    ) : ClientGeneration
}
