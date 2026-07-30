package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Serialized AsyncAPI document format selected by a document-only generator profile.
 *
 * Expected behavior is covered by:
 * - `DocumentFormatTest`
 */
enum class DocumentFormat(
    val configurationValue: String,
) {
    YAML("yaml"),
    JSON("json"),
    ;

    companion object {
        val supportedConfigurationValues: List<String> = entries.map { it.configurationValue }

        fun fromConfigurationValue(
            value: String,
            path: String,
        ): DocumentFormat =
            entries.firstOrNull { it.configurationValue == value }
                ?: throw IllegalArgumentException(
                    "Invalid $path '$value'. Supported values: ${supportedConfigurationValues.joinToString(", ")}",
                )
    }
}
