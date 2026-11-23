package dev.banking.asyncapi.generator.core.generator.configuration

private const val CONFIGURATION_WARNINGS =
    "https://github.com/evryfs/asyncapi-generator/blob/main/docs/reference/" +
        "generator-configuration.md#configuration-warnings"

/**
 * Configuration that is valid but has no effect on this execution.
 *
 * Frontends print it with [GeneratorConfiguration.formatWarnings], in the same layout as
 * contract validation warnings. Users silence a warning by listing its
 * [ConfigurationWarningId.configurationValue] in `suppressedWarnings`.
 *
 * @param id the identifier users list in `suppressedWarnings`.
 * @param message what is ignored and why.
 */
data class ConfigurationWarning(
    val id: ConfigurationWarningId,
    val message: String,
) {
    /** The documentation link of [id]. */
    val documentation: String
        get() = id.documentation
}

/**
 * Stable identifiers of configuration warnings, as users write them in `suppressedWarnings`.
 *
 * @param configurationValue the value users write in `suppressedWarnings`.
 * @param documentation the link to the warning's documentation.
 */
enum class ConfigurationWarningId(
    val configurationValue: String,
    val documentation: String,
) {
    /** Shared client configuration on an execution that generates no client. */
    UNUSED_CLIENT_CONFIG("unused-client-config", CONFIGURATION_WARNINGS),
    ;

    companion object {
        val supportedConfigurationValues: List<String> = entries.map { it.configurationValue }

        /** The identifier named by [value]; [path] names the setting in errors. */
        fun fromConfigurationValue(
            value: String,
            path: String,
        ): ConfigurationWarningId =
            entries.firstOrNull { it.configurationValue == value }
                ?: throw IllegalArgumentException(
                    "Invalid $path '$value'. Supported values: ${supportedConfigurationValues.joinToString(", ")}",
                )
    }
}
