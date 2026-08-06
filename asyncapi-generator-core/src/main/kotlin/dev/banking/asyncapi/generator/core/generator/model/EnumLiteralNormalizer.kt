package dev.banking.asyncapi.generator.core.generator.model

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.EnumLiteralCollision
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidEnum

internal class EnumLiteralNormalizer(
    private val packageName: String,
) {
    private val identifierRegex = Regex("^[A-Z_][A-Z0-9_]*$")

    fun normalize(
        schemaName: String,
        rawValues: List<Any?>,
    ): List<String> {
        val originalsByNormalizedValue = linkedMapOf<String, MutableList<String>>()
        val normalizedValues =
            rawValues.map { rawValue ->
                val original = rawValue.toEnumLiteral()
                val normalized = original.uppercase()
                if (!identifierRegex.matches(normalized)) {
                    throw InvalidEnum(
                        schemaName = schemaName,
                        literal = original,
                        packageName = packageName,
                    )
                }
                originalsByNormalizedValue.getOrPut(normalized) { mutableListOf() }.add(original)
                normalized
            }

        originalsByNormalizedValue.forEach { (normalized, originals) ->
            if (originals.size > 1) {
                throw EnumLiteralCollision(
                    schemaName = schemaName,
                    originals = originals,
                    normalized = normalized,
                    packageName = packageName,
                )
            }
        }
        return normalizedValues
    }

    private fun Any?.toEnumLiteral(): String =
        toString()
            .trimStart('"', '\'', '|', '>')
            .removeSurrounding("\"")
}
