package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Schema artifact format selected by user-facing schema configuration.
 *
 * Schema type inference is performed when the complete generator configuration
 * and parsed AsyncAPI contract are available.
 *
 * Expected behavior is covered by:
 * - `SchemaTypeTest`
 */
enum class SchemaType(
    val configurationValue: String,
) {
    JSON_SCHEMA("json-schema"),
    AVRO("avro"),
    PROTOBUF("protobuf"),
    ;

    companion object {
        val supportedConfigurationValues: List<String> = entries.map { it.configurationValue }

        fun fromConfigurationValue(
            value: String,
            path: String,
        ): SchemaType =
            entries.firstOrNull { it.configurationValue == value }
                ?: throw IllegalArgumentException(
                    "Invalid $path '$value'. Supported values: ${supportedConfigurationValues.joinToString(", ")}",
                )
    }
}
