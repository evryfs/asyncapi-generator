package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import dev.banking.asyncapi.generator.core.validator.util.DiagnosticLayout

/**
 * Typed core configuration consumed by generator planning and generation.
 *
 * Entry points such as CLI, Maven, and Gradle should translate their public
 * configuration shape into this model before invoking core generator behavior.
 */
data class GeneratorConfiguration(
    val profile: GeneratorProfile,
    val output: GeneratorOutputConfiguration,
    val models: ModelGeneration = ModelGeneration.Disabled,
    val schemas: List<SchemaGeneration> = emptyList(),
    val clients: List<ClientGeneration> = emptyList(),
    val warnings: List<ConfigurationWarning> = emptyList(),
) {
    val sourceLanguage: SourceLanguage?
        get() = (profile as? GeneratorProfile.Source)?.language

    /** [warnings] in the layout of contract validation warnings, or an empty string when there are none. */
    fun formatWarnings(): String =
        if (warnings.isEmpty()) {
            ""
        } else {
            DiagnosticLayout.format(
                title = "Configuration has ${warnings.size} warning(s):",
                entries = warnings.map { DiagnosticLayout.Entry(it.message, snippet = null, it.documentation) },
            )
        }

    fun hasConfiguredOutputs(): Boolean =
        output.document != null ||
            models != ModelGeneration.Disabled ||
            schemas.isNotEmpty() ||
            clients.isNotEmpty()
}
