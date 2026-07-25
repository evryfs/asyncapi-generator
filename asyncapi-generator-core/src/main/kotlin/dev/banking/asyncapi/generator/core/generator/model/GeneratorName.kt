package dev.banking.asyncapi.generator.core.generator.model

import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorProfile

/**
 * Generator implementation selected by user-facing configuration.
 *
 * Expected behavior is covered by:
 * - `GeneratorNameTest`
 */
enum class GeneratorName(
    val configurationValue: String,
    val profile: GeneratorProfile,
) {
    JAVA(
        configurationValue = "java",
        profile = GeneratorProfile.Source(SourceLanguage.JAVA),
    ),
    KOTLIN(
        configurationValue = "kotlin",
        profile = GeneratorProfile.Source(SourceLanguage.KOTLIN),
    ),
    ;

    companion object {
        val supportedConfigurationValues: List<String> = entries.map { it.configurationValue }

        fun fromConfigurationValue(
            value: String?,
            path: String,
        ): GeneratorName {
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
