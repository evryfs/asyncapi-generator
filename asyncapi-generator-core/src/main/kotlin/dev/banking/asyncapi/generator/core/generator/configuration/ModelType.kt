package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Payload model implementation selected by user-facing model configuration.
 *
 * The default model type depends on the selected source generator and is
 * therefore applied when the complete generator configuration is resolved.
 */
enum class ModelType(
    val configurationValue: String,
) {
    KOTLIN_DATA_CLASS("kotlin-data-class"),
    JAVA_CLASS("java-class"),
    JAVA_RECORD("java-record"),
    AVRO_SPECIFIC_RECORD("avro-specific-record"),
    PROTOBUF_MESSAGE("protobuf-message"),
    ;

    companion object {
        val supportedConfigurationValues: List<String> = entries.map { it.configurationValue }

        fun fromConfigurationValue(
            value: String,
            path: String,
        ): ModelType =
            entries.firstOrNull { it.configurationValue == value }
                ?: throw IllegalArgumentException(
                    "Invalid $path '$value'. Supported values: ${supportedConfigurationValues.joinToString(", ")}",
                )
    }
}
