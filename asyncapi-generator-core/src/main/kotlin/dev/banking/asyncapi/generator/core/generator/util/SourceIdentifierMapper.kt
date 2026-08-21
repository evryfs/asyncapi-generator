package dev.banking.asyncapi.generator.core.generator.util

/**
 * Converts wire-level names (property names, header names, etc.) into valid
 * Java and Kotlin source identifiers.
 *
 * Names that are already valid identifiers are preserved as-is. Names with
 * hyphens, spaces, leading digits, or other invalid characters are converted
 * to lowerCamelCase. Reserved words are suffixed with an underscore.
 */
internal object SourceIdentifierMapper {

    private val validIdentifierRegex = Regex("[A-Za-z_][A-Za-z0-9_]*")

    private val reservedIdentifiers = setOf(
        "abstract", "actual", "annotation", "as", "assert", "boolean", "break",
        "by", "byte", "case", "catch", "char", "class", "companion", "const",
        "constructor", "continue", "crossinline", "data", "default", "delegate",
        "do", "double", "dynamic", "else", "enum", "expect", "extends",
        "external", "false", "field", "final", "finally", "float", "for", "fun",
        "get", "goto", "if", "implements", "import", "in", "infix", "init",
        "inline", "inner", "instanceof", "int", "interface", "internal", "is",
        "lateinit", "long", "native", "new", "noinline", "null", "object",
        "open", "operator", "out", "override", "package", "param", "permits",
        "private", "property", "protected", "public", "receiver", "record",
        "reified", "return", "sealed", "set", "setparam", "short", "static",
        "strictfp", "super", "suspend", "switch", "synchronized", "tailrec",
        "this", "throw", "throws", "transient", "true", "try", "typealias",
        "typeof", "val", "var", "vararg", "void", "volatile", "when", "where",
        "while", "yield",
    )

    fun toIdentifier(wireName: String): String {
        if (validIdentifierRegex.matches(wireName) && wireName !in reservedIdentifiers) {
            return wireName
        }

        val camelCase = MapperUtil.toCamelCase(wireName)
        if (camelCase.isEmpty()) return "_"

        val identifier = if (camelCase[0].isDigit()) "_$camelCase" else camelCase
        return if (identifier in reservedIdentifiers) "${identifier}_" else identifier
    }
}
