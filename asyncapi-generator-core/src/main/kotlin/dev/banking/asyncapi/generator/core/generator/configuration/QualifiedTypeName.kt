package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Validated fully qualified source type name supplied by generator configuration.
 */
@JvmInline
value class QualifiedTypeName private constructor(
    val value: String,
) {
    val simpleName: String
        get() = value.substringAfterLast('.')

    companion object {
        private val pattern = Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+")

        fun fromConfigurationValue(
            value: String,
            path: String,
        ): QualifiedTypeName {
            if (value.isBlank()) {
                throw IllegalArgumentException("$path cannot be empty")
            }

            if (!pattern.matches(value)) {
                throw IllegalArgumentException(
                    "$path must be a fully qualified type name, for example com.example.GeneratedPayload",
                )
            }

            return QualifiedTypeName(value)
        }
    }
}
