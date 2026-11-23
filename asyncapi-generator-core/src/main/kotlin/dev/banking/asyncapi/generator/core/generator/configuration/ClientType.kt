package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Client technology selected by user-facing generator configuration.
 */
enum class ClientType(
    val configurationValue: String,
) {
    SPRING_KAFKA("spring-kafka"),
    ;

    companion object {
        val supportedConfigurationValues: List<String> = entries.map { it.configurationValue }

        /** Used when the setting is omitted: the only client technology. */
        val DEFAULT = SPRING_KAFKA

        fun fromConfigurationValue(
            value: String?,
            path: String,
        ): ClientType {
            if (value == null) return DEFAULT
            return entries.firstOrNull { it.configurationValue == value }
                ?: throw IllegalArgumentException(
                    "Invalid $path '$value'. Supported values: ${supportedConfigurationValues.joinToString(", ")}",
                )
        }
    }
}
