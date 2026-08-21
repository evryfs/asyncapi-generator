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
            require(value.isNotBlank()) { "$path cannot be empty" }
            require(pattern.matches(value)) { "$path must be a dot-separated package name, for example com.example.model" }

            return PackageName(value)
        }
    }
}
