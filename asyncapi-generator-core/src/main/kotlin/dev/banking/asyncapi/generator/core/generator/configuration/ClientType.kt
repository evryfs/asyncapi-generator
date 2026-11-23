package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Client technology selected by user-facing generator configuration.
 *
 * @param configurationValue the value users write in `clientConfig.clientType`.
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

        /** The value named by [value], or [DEFAULT] when it is omitted; [path] names the setting in errors. */
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
