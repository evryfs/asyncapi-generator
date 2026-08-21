package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Validated package name used by generated source and schema artifacts.
 */
@JvmInline
value class PackageName private constructor(
    val value: String,
) {
    companion object {
        private val pattern = Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*")

        fun fromConfigurationValue(
            value: String,
            path: String,
        ): PackageName {
            if (value.isBlank()) {
                throw IllegalArgumentException("$path cannot be empty")
            }

            if (!pattern.matches(value)) {
                throw IllegalArgumentException(
                    "$path must be a dot-separated package name, for example com.example.model",
                )
            }

            return PackageName(value)
        }
    }
}
