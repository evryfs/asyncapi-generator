package dev.banking.asyncapi.generator.core.generator.util

/** Escapes exact model values for generated Java and Kotlin quoted string literals. */
internal object SourceLiteralEscaper {

    fun forJava(value: String): String = escape(value, escapeDollar = false)

    fun forKotlin(value: String): String = escape(value, escapeDollar = true)

    private fun escape(value: String, escapeDollar: Boolean): String = buildString {
        value.forEachIndexed { index, character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\t' -> append("\\t")
                '\n' -> append("\\n")
                '\u000C' -> append("\\f")
                '\r' -> append("\\r")
                '$' -> {
                    val next = value.getOrNull(index + 1)
                    if (escapeDollar && (next == '{' || next?.let(Character::isJavaIdentifierStart) == true)) {
                        append("\\$")
                    } else {
                        append(character)
                    }
                }
                else -> {
                    if (character.code < 0x20) {
                        append("\\u")
                        append(character.code.toString(16).padStart(4, '0'))
                    } else {
                        append(character)
                    }
                }
            }
        }
    }
}
