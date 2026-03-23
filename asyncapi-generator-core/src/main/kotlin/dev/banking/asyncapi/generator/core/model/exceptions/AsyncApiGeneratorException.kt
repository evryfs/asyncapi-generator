package dev.banking.asyncapi.generator.core.model.exceptions

sealed class AsyncApiGeneratorException(
    message: String,
) : Exception(message) {
    class EmptyLanguageList : AsyncApiGeneratorException("The language list cannot be empty")

    class NullComponents : AsyncApiGeneratorException("The Components object cannot be null")

    class UnsupportedLanguage(
        language: String,
    ) : AsyncApiGeneratorException("The language $language is not supported")

    class InvalidEnum(
        schemaName: String,
        literal: String,
        packageName: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Enum generation failed for schema '$schemaName'. Invalid enum literal: '$literal'")
                appendLine("Enum constants must match [A-Z_][A-Z0-9_]*. Target output: $packageName.$schemaName.kt")
                appendLine()
            }.trimEnd(),
        )

    class EnumLiteralCollision(
        schemaName: String,
        originals: List<String>,
        normalized: String,
        packageName: String,
    ) : AsyncApiGeneratorException(
            buildString {
                appendLine()
                appendLine("Enum generation failed for schema '$schemaName'. Target output: $packageName.$schemaName.kt")
                appendLine("Enum literals collide after normalization: ${formatOriginals(originals)} -> '$normalized'")
                appendLine()
            }.trimEnd(),
        ) {
        companion object {
            private fun formatOriginals(values: List<String>): String = values.joinToString(prefix = "[", postfix = "]") { "'$it'" }
        }
    }
}
