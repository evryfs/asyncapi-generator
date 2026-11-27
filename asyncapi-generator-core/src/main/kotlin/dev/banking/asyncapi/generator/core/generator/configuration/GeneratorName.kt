package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Generator implementation selected by user-facing configuration.
 *
 * @param configurationValue the value users write in `generatorName`.
 * @param profile what the generator produces.
 */
internal enum class GeneratorName(
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

    /** The language a source generator writes, or `null` for a schema or document generator. */
    val sourceLanguage: SourceLanguage?
        get() = (profile as? GeneratorProfile.Source)?.language

    companion object {
        val supportedConfigurationValues: List<String> = entries.map { it.configurationValue }

        /** The generator named by [value]; [path] names the setting in errors. */
        fun fromConfigurationValue(
            value: String?,
            path: String,
        ): GeneratorName {
            requireNotNull(value) { "$path is required" }
            return entries.firstOrNull { it.configurationValue == value }
                ?: throw IllegalArgumentException(
                    "Invalid $path '$value'. Supported values: ${supportedConfigurationValues.joinToString(", ")}",
                )
        }
    }
}
