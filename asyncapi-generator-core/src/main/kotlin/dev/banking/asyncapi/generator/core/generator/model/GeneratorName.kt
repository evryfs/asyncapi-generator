package dev.banking.asyncapi.generator.core.generator.model

/**
 * Generator implementation selected by user-facing configuration.
 *
 * Expected behavior is covered by:
 * - `GeneratorNameTest`
 */
enum class GeneratorName(
    val configurationValue: String,
    val sourceLanguage: SourceLanguage,
) {
    JAVA("java", SourceLanguage.JAVA),
    KOTLIN("kotlin", SourceLanguage.KOTLIN),
    AVRO("avro", SourceLanguage.JAVA),
    PROTOBUF("protobuf", SourceLanguage.JAVA),
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
