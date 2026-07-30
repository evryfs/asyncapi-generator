package dev.banking.asyncapi.generator.core.generator.model

/**
 * Source language used by resolved generation tasks.
 *
 * Expected behavior is covered by:
 * - `SourceLanguageTest`
 */
enum class SourceLanguage(
    val configurationValue: String,
) {
    KOTLIN("kotlin"),
    JAVA("java"),
    ;

    companion object {
        val supportedConfigurationValues: List<String> = entries.map { it.configurationValue }

        fun fromConfigurationValue(
            value: String?,
            path: String,
        ): SourceLanguage {
            if (value == null) {
                return KOTLIN
            }

            return entries.firstOrNull { it.configurationValue == value }
                ?: throw IllegalArgumentException(
                    "Invalid $path '$value'. Supported values: ${supportedConfigurationValues.joinToString(", ")}",
                )
        }
    }
}
