package dev.banking.asyncapi.generator.core.generator.model

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.EnumLiteralCollision
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidEnum

/**
 * One enum constant: its source name and the value it has on the wire.
 *
 * @property jsonValue The wire value when it differs from [name], so generated code maps it with
 *   `@JsonProperty`; null when Jackson's default (the constant name) already matches.
 */
data class EnumConstant(
    val name: String,
    val jsonValue: String?,
)

/**
 * Turns enum values from the contract into Kotlin and Java enum constants.
 *
 * Constants are upper case; characters that cannot appear in an identifier become `_`, so
 * `in-progress` becomes `IN_PROGRESS`. The original value is kept as the wire value, so
 * messages keep using the contract's values. Values that leave no identifier, or that
 * collide after this mapping, fail.
 */
internal class EnumLiteralNormalizer(
    private val packageName: String,
) {
    fun normalize(
        schemaName: String,
        rawValues: List<Any?>,
    ): List<EnumConstant> {
        val originalsByConstant = linkedMapOf<String, MutableList<String>>()
        val constants =
            rawValues.map { rawValue ->
                val original = rawValue.toEnumLiteral()
                val constant = constantName(original)
                    ?: throw InvalidEnum(schemaName = schemaName, literal = original, packageName = packageName)
                originalsByConstant.getOrPut(constant) { mutableListOf() }.add(original)
                EnumConstant(name = constant, jsonValue = original.takeIf { it != constant })
            }

        originalsByConstant.forEach { (constant, originals) ->
            if (originals.size > 1) {
                throw EnumLiteralCollision(
                    schemaName = schemaName,
                    originals = originals,
                    normalized = constant,
                    packageName = packageName,
                )
            }
        }
        return constants
    }

    private fun Any?.toEnumLiteral(): String =
        toString()
            .trimStart('"', '\'', '|', '>')
            .removeSurrounding("\"")

    companion object {
        private val separators = Regex("[^A-Z0-9_]+")

        /** The constant for one enum value, or null when no identifier remains. */
        fun constantName(value: String): String? {
            val constant = value.uppercase().replace(separators, "_")
            if (constant.all { it == '_' }) return null
            return if (constant.first().isDigit()) "_$constant" else constant
        }
    }
}
