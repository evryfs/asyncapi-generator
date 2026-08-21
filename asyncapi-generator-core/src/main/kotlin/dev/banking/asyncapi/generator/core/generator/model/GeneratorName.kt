package dev.banking.asyncapi.generator.core.generator.model

import dev.banking.asyncapi.generator.core.generator.configuration.DocumentFormat
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorProfile
import dev.banking.asyncapi.generator.core.generator.configuration.SchemaType

/**
 * Generator implementation selected by user-facing configuration.
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
    AVRO_SCHEMA(
        configurationValue = "avro-schema",
        profile = GeneratorProfile.Schema(SchemaType.AVRO),
    ),
    PROTOBUF_SCHEMA(
        configurationValue = "protobuf-schema",
        profile = GeneratorProfile.Schema(SchemaType.PROTOBUF),
    ),
    JSON_SCHEMA(
        configurationValue = "json-schema",
        profile = GeneratorProfile.Schema(SchemaType.JSON_SCHEMA),
    ),
    ASYNCAPI_YAML(
        configurationValue = "asyncapi-yaml",
        profile = GeneratorProfile.Document(DocumentFormat.YAML),
    ),
    ASYNCAPI_JSON(
        configurationValue = "asyncapi-json",
        profile = GeneratorProfile.Document(DocumentFormat.JSON),
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
