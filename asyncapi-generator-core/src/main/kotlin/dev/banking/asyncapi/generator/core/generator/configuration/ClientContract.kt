package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Generated client contract shape selected by user-facing generator configuration.
 *
 * @param configurationValue the value users write in `clientConfig.clientContract`.
 */
internal enum class ClientContract(
    val configurationValue: String,
) {
    INTERFACE("interface"),
    ;

    companion object {
        val supportedConfigurationValues: List<String> = entries.map { it.configurationValue }

        /** Used when the setting is omitted: the only contract shape. */
        val DEFAULT = INTERFACE

        /** The value named by [value], or [DEFAULT] when it is omitted; [path] names the setting in errors. */
        fun fromConfigurationValue(
            value: String?,
            path: String,
        ): ClientContract {
            if (value == null) return DEFAULT
            return entries.firstOrNull { it.configurationValue == value }
                ?: throw IllegalArgumentException(
                    "Invalid $path '$value'. Supported values: ${supportedConfigurationValues.joinToString(", ")}",
                )
        }
    }
}
