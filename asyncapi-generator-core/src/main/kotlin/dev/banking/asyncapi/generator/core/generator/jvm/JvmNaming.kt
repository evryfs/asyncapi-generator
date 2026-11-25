package dev.banking.asyncapi.generator.core.generator.jvm

import dev.banking.asyncapi.generator.core.generator.sourcemodel.NameCase
import dev.banking.asyncapi.generator.core.generator.sourcemodel.SourceNaming

/**
 * Names for Kotlin and Java sources. Both languages share one set of rules, so a contract
 * generates the same identifiers and JSON in either language.
 *
 * Property names that are already valid identifiers are kept; other names become lowerCamelCase.
 * Words reserved in Kotlin or Java get a `_` suffix. Enum constants are UPPER_SNAKE_CASE.
 */
internal object JvmNaming : SourceNaming {
    private val separators = Regex("[^A-Z0-9_]+")
    private val validIdentifierRegex = Regex("[A-Za-z_][A-Za-z0-9_]*")

    private val reservedIdentifiers =
        setOf(
            "abstract",
            "actual",
            "annotation",
            "as",
            "assert",
            "boolean",
            "break",
            "by",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "companion",
            "const",
            "constructor",
            "continue",
            "crossinline",
            "data",
            "default",
            "delegate",
            "do",
            "double",
            "dynamic",
            "else",
            "enum",
            "expect",
            "extends",
            "external",
            "false",
            "field",
            "final",
            "finally",
            "float",
            "for",
            "fun",
            "get",
            "goto",
            "if",
            "implements",
            "import",
            "in",
            "infix",
            "init",
            "inline",
            "inner",
            "instanceof",
            "int",
            "interface",
            "internal",
            "is",
            "lateinit",
            "long",
            "native",
            "new",
            "noinline",
            "null",
            "object",
            "open",
            "operator",
            "out",
            "override",
            "package",
            "param",
            "permits",
            "private",
            "property",
            "protected",
            "public",
            "receiver",
            "record",
            "reified",
            "return",
            "sealed",
            "set",
            "setparam",
            "short",
            "static",
            "strictfp",
            "super",
            "suspend",
            "switch",
            "synchronized",
            "tailrec",
            "this",
            "throw",
            "throws",
            "transient",
            "true",
            "try",
            "typealias",
            "typeof",
            "val",
            "var",
            "vararg",
            "void",
            "volatile",
            "when",
            "where",
            "while",
            "yield",
        )

    /** Whether [identifier] is a Kotlin or Java keyword. */
    fun isReserved(identifier: String): Boolean = identifier in reservedIdentifiers

    override fun propertyIdentifier(wireName: String): String =
        if (validIdentifierRegex.matches(wireName) && wireName !in reservedIdentifiers) {
            wireName
        } else {
            camelCaseIdentifier(wireName)
        }

    private fun camelCaseIdentifier(wireName: String): String {
        val camelCase = NameCase.toCamelCase(wireName)
        val identifier =
            when {
                camelCase.isEmpty() -> "_"
                camelCase.first().isDigit() -> "_$camelCase"
                else -> camelCase
            }
        return if (identifier in reservedIdentifiers) "${identifier}_" else identifier
    }

    override fun enumConstant(value: String): String? {
        val constant = value.uppercase().replace(separators, "_")
        if (constant.all { it == '_' }) return null
        return if (constant.first().isDigit()) "_$constant" else constant
    }
}
