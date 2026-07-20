package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Generated Protobuf model API selected by user-facing model configuration.
 *
 * `KOTLIN` generates the Java Protobuf messages required at runtime together
 * with the official Kotlin DSL sources.
 *
 * Expected behavior is covered by:
 * - `ProtobufModelTypeTest`
 */
enum class ProtobufModelType(
    val configurationValue: String,
) {
    JAVA("java"),
    KOTLIN("kotlin"),
    ;

    companion object {
        val supportedConfigurationValues: List<String> = entries.map { it.configurationValue }

        fun fromConfigurationValue(
            value: String?,
            path: String,
        ): ProtobufModelType {
            if (value == null) {
                return JAVA
            }

            return entries.firstOrNull { it.configurationValue == value }
                ?: throw IllegalArgumentException(
                    "Invalid $path '$value'. Supported values: ${supportedConfigurationValues.joinToString(", ")}",
                )
        }
    }
}
