package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.diagnostics.DiagnosticLayout

/**
 * Typed core configuration consumed by generator planning and generation.
 *
 * [GeneratorConfigurationFactory] creates it from [GeneratorSettings].
 *
 * @param profile what the generator produces: source code, schemas or a document.
 * @param output where generated files are written.
 * @param models the source models to generate.
 * @param schemas the schema artifacts to generate.
 * @param client the client contracts to generate, or `null` for none.
 * @param warnings valid settings that have no effect on this execution.
 */
data class GeneratorConfiguration(
    val profile: GeneratorProfile,
    val output: GeneratorOutputConfiguration,
    val models: ModelGeneration = ModelGeneration.Disabled,
    val schemas: List<SchemaGeneration> = emptyList(),
    val client: ClientGeneration? = null,
    val warnings: List<ConfigurationWarning> = emptyList(),
) {
    /** The language of a source profile, or `null` for a schema or document profile. */
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

    /** Whether the configuration generates anything at all. */
    fun hasConfiguredOutputs(): Boolean =
        output.document != null ||
            models != ModelGeneration.Disabled ||
            schemas.isNotEmpty() ||
            client != null
}
