package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage

/**
 * Typed core configuration consumed by generator planning and generation.
 *
 * Entry points such as CLI, Maven, and Gradle should translate their public
 * configuration shape into this model before invoking core generator behavior.
 *
 * Expected behavior is covered by:
 * - `GenerationPlannerTest`
 */
data class GeneratorConfiguration(
    val language: SourceLanguage,
    val output: GeneratorOutputConfiguration,
    val models: ModelGeneration = ModelGeneration.Disabled,
    val schemas: List<SchemaGeneration> = emptyList(),
    val clients: List<ClientGeneration> = emptyList(),
) {
    fun hasConfiguredOutputs(): Boolean =
        models != ModelGeneration.Disabled ||
            schemas.isNotEmpty() ||
            clients.isNotEmpty()
}
