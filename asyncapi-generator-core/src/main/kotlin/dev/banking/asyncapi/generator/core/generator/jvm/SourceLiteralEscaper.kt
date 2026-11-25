package dev.banking.asyncapi.generator.core.generator.jvm

/** Escapes exact model values for generated Java and Kotlin quoted string literals. */
internal object SourceLiteralEscaper {
    fun forJava(value: String): String = escape(value, escapeDollar = false)

    fun forKotlin(value: String): String = escape(value, escapeDollar = true)

    private fun escape(
        value: String,
        escapeDollar: Boolean,
    ): String =
        buildString {
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
                        if (character.code < FIRST_PRINTABLE) {
                            append("\\u")
                            append(character.code.toString(HEX_RADIX).padStart(UNICODE_ESCAPE_DIGITS, '0'))
                        } else {
                            append(character)
                        }
                    }
                }
            }
        }

    /** Characters below this code are control characters, written as `\u` escapes. */
    private const val FIRST_PRINTABLE = 0x20
    private const val HEX_RADIX = 16
    private const val UNICODE_ESCAPE_DIGITS = 4
}
