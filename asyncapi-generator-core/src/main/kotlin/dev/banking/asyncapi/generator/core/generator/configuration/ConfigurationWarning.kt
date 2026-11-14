package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Configuration that is valid but has no effect on this execution.
 *
 * Frontends print the [message]. Users silence a warning by listing its
 * [ConfigurationWarningId.configurationValue] in `suppressedWarnings`.
 */
data class ConfigurationWarning(
    val id: ConfigurationWarningId,
    val message: String,
)

/**
 * Stable identifiers of configuration warnings, as users write them in `suppressedWarnings`.
 */
enum class ConfigurationWarningId(
    val configurationValue: String,
) {
    /** Shared client configuration on an execution that generates no client. */
    UNUSED_CLIENT_CONFIG("unused-client-config"),
    ;

    companion object {
        val supportedConfigurationValues: List<String> = entries.map { it.configurationValue }

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
