package dev.banking.asyncapi.generator.core.generator.sourcemodel

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.EnumLiteralCollision
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidEnum

/**
 * One enum constant: its source name and the value it has on the wire.
 *
 * @property name Name of the constant in generated sources.
 * @property jsonValue The wire value when it differs from [name], so generated code maps it with
 *   `@JsonProperty`; null when Jackson's default (the constant name) already matches.
 */
data class EnumConstant(
    val name: String,
    val jsonValue: String?,
)

/**
 * Turns enum values from the contract into enum constants named by [naming].
 *
 * The original value is kept as the wire value, so messages keep using the contract's values.
 * Values that leave no identifier, or that collide after naming, fail.
 *
 * @param packageName Package of the generated enums, for error messages.
 * @param naming Naming rules of the generated sources.
 */
internal class EnumLiteralNormalizer(
    private val packageName: String,
    private val naming: SourceNaming,
) {
    fun normalize(
        schemaName: String,
        rawValues: List<Any?>,
    ): List<EnumConstant> {
        val originalsByConstant = linkedMapOf<String, MutableList<String>>()
        val constants =
            rawValues.map { rawValue ->
                val original = rawValue.toString()
                val constant =
                    naming.enumConstant(original)
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
}
