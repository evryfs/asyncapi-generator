package dev.banking.asyncapi.generator.core.generator.sourcemodel

import dev.banking.asyncapi.generator.core.model.schemas.Schema

/** The validation constraints of a property or parameter, independent of how a language expresses them. */
internal sealed interface Constraints {
    /**
     * The constraints of a string.
     *
     * @property minLength Shortest allowed length; null without a bound.
     * @property maxLength Longest allowed length; null without a bound.
     * @property pattern Regular expression the value must match; null without one.
     * @property email Whether the value must be an email address.
     */
    data class Text(
        val minLength: Int?,
        val maxLength: Int?,
        val pattern: String?,
        val email: Boolean,
    ) : Constraints

    /**
     * The inclusive bounds of an integer.
     *
     * @property minimum Smallest allowed value; null without a bound.
     * @property maximum Largest allowed value; null without a bound.
     */
    data class WholeNumber(
        val minimum: Long?,
        val maximum: Long?,
    ) : Constraints

    /**
     * The bounds of a decimal number.
     *
     * @property minimum Lower bound; null without one.
     * @property maximum Upper bound; null without one.
     */
    data class Decimal(
        val minimum: DecimalBound?,
        val maximum: DecimalBound?,
    ) : Constraints

    /** No constraints. */
    data object None : Constraints

    companion object {
        /** The constraints [schema] declares for its type; [None] without a schema. */
        fun of(schema: Schema?): Constraints = schema?.let(::declaredBy) ?: None

        private fun declaredBy(schema: Schema): Constraints =
            when (schema.type?.primary) {
                "string" ->
                    Text(
                        minLength = schema.minLength?.toInt(),
                        maxLength = schema.maxLength?.toInt(),
                        pattern = schema.pattern,
                        email = schema.format == "email",
                    )
                "integer" -> WholeNumber(minimum = schema.minimum?.toLong(), maximum = schema.maximum?.toLong())
                "number" ->
                    Decimal(
                        minimum = DecimalBound.of(exclusive = schema.exclusiveMinimum, inclusive = schema.minimum),
                        maximum = DecimalBound.of(exclusive = schema.exclusiveMaximum, inclusive = schema.maximum),
                    )
                else -> None
            }
    }
}

/**
 * One bound of a decimal number.
 *
 * @property value The bound as written in the contract.
 * @property inclusive Whether the bound itself is allowed.
 */
internal data class DecimalBound(
    val value: String,
    val inclusive: Boolean,
) {
    companion object {
        /** The exclusive bound when there is one, which is stricter, and otherwise the inclusive one. */
        fun of(
            exclusive: Number?,
            inclusive: Number?,
        ): DecimalBound? =
            exclusive?.let { DecimalBound(it.toString(), inclusive = false) }
                ?: inclusive?.let { DecimalBound(it.toString(), inclusive = true) }
    }
}

/** Whether a property is only read or only written, from `readOnly` and `writeOnly`. */
internal enum class PropertyAccess {
    READ_ONLY,
    WRITE_ONLY,
    READ_WRITE,
    ;

    companion object {
        /** The access [schema] declares; null when it declares neither `readOnly` nor `writeOnly`. */
        fun of(schema: Schema?): PropertyAccess? =
            when {
                schema?.readOnly == true && schema.writeOnly == true -> READ_WRITE
                schema?.readOnly == true -> READ_ONLY
                schema?.writeOnly == true -> WRITE_ONLY
                else -> null
            }
    }
}
