package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Client technology selected by user-facing generator configuration.
 *
 * Expected behavior is covered by:
 * - `ClientTypeTest`
 */
enum class ClientType(
    val configurationValue: String,
) {
    SPRING_KAFKA("spring-kafka"),
    ;

    companion object {
        val supportedConfigurationValues: List<String> = entries.map { it.configurationValue }

        fun fromConfigurationValue(
            value: String?,
            path: String,
        ): ClientType {
            if (value == null) {
                throw IllegalArgumentException("$path is required")
            }

            return entries.firstOrNull { it.configurationValue == value }
                ?: throw IllegalArgumentException(
                    "Invalid $path '$value'. Supported values: ${supportedConfigurationValues.joinToString(", ")}",
                )
        }
    }
}
