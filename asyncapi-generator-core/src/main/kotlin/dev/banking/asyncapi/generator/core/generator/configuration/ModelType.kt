package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Payload model implementation selected by user-facing model configuration.
 *
 * The default model type depends on the selected source generator and is
 * therefore applied when the complete generator configuration is resolved.
 *
 * @param configurationValue the value users write in `modelConfig.modelType`.
 * @param language the only language whose generator writes this model type, or
 *   `null` for a native Avro or Protobuf model that every source language supports.
 * @param supportsAnnotation whether `modelConfig.modelAnnotation` can be placed on
 *   the generated models.
 */
internal enum class ModelType(
    val configurationValue: String,
    val language: SourceLanguage?,
    val supportsAnnotation: Boolean,
) {
    KOTLIN_DATA_CLASS("kotlin-data-class", SourceLanguage.KOTLIN, supportsAnnotation = true),
    JAVA_CLASS("java-class", SourceLanguage.JAVA, supportsAnnotation = true),
    JAVA_RECORD("java-record", SourceLanguage.JAVA, supportsAnnotation = true),
    AVRO_SPECIFIC_RECORD("avro-specific-record", language = null, supportsAnnotation = false),
    PROTOBUF_MESSAGE("protobuf-message", language = null, supportsAnnotation = false),
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
