package dev.banking.asyncapi.generator.core.model.exceptions

sealed class AsyncApiGeneratorException(message: String) : Exception(message) {
    class EmptyLanguageList :
        AsyncApiGeneratorException("The language list cannot be empty")

    class NullComponents :
        AsyncApiGeneratorException("The Components object cannot be null")

    class UnsupportedLanguage(language: String) :
        AsyncApiGeneratorException("The language $language is not supported")

    class InvalidKotlinEnumLiteral(
        schemaName: String,
        literal: String,
        normalized: String,
        packageName: String,
    ) : AsyncApiGeneratorException(
        buildString {
            appendLine("Kotlin enum generation failed for schema '$schemaName'")
            appendLine("Invalid enum literal: '$literal'")
            appendLine("Would generate invalid enum constant: '$normalized'")
            appendLine("Rule: Kotlin enum constants must match [A-Z_][A-Z0-9_]*")
            appendLine("Target output: $packageName.$schemaName.kt")
        }.trimEnd()
    )
    class KotlinEnumLiteralCollision(
        schemaName: String,
        originals: List<String>,
        normalized: String,
        packageName: String,
    ) : AsyncApiGeneratorException(
        buildString {
            appendLine("Kotlin enum generation failed for schema '$schemaName'")
            appendLine("Enum literals collide after normalization: ${formatOriginals(originals)} -> '$normalized'")
            appendLine("Target output: $packageName.$schemaName.kt")
        }.trimEnd()
    ) {
        companion object {
            private fun formatOriginals(values: List<String>): String =
                values.joinToString(prefix = "[", postfix = "]") { "'$it'" }
        }
    }
}
