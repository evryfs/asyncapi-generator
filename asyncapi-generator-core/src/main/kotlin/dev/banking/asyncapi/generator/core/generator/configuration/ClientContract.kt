package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Generated client contract shape selected by user-facing generator configuration.
 */
enum class ClientContract(
    val configurationValue: String,
) {
    INTERFACE("interface"),
    ;

    companion object {
        val supportedConfigurationValues: List<String> = entries.map { it.configurationValue }

        fun fromConfigurationValue(
            value: String?,
            path: String,
        ): ClientContract {
            requireNotNull(value) { "$path is required" }
            return entries.firstOrNull { it.configurationValue == value }
                ?: throw IllegalArgumentException(
                    "Invalid $path '$value'. Supported values: ${supportedConfigurationValues.joinToString(", ")}",
                )
        }
    }
}
