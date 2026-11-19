package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import java.math.BigDecimal

/**
 * Validates the paired lower and upper bounds that Schema Object keywords declare, such as `minLength` and
 * `maxLength`.
 *
 * @param asyncApiContext the load the schemas belong to
 */
internal class SchemaBounds(
    private val asyncApiContext: AsyncApiContext,
) {
    /**
     * One bound a schema declares.
     *
     * @property keyword the keyword that declares the bound
     * @property value the declared value, or `null` when the keyword is absent
     */
    class Bound(
        val keyword: String,
        val value: Number?,
    )

    /**
     * Reports a [bound] that is not a non-negative integer under [rule].
     *
     * @return the bound as a decimal, or `null` when it is absent or invalid
     */
    fun nonNegativeInteger(
        schema: CheckedSchema,
        bound: Bound,
        rule: ValidationRule,
    ): BigDecimal? {
        val value = bound.value ?: return null
        val decimal = SchemaValueSemantics.decimal(value)
        val valid = decimal != null && decimal.signum() >= 0 && decimal.stripTrailingZeros().scale() <= 0
        if (!valid) {
            schema.results.error(
                rule,
                "${schema.path} '${bound.keyword}' must be a non-negative integer. Found: $value.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(schema.node, bound.keyword),
            )
        }
        return decimal.takeIf { valid }
    }

    /** Reports under [rule] a [lower] bound that is greater than its [upper] bound. */
    fun orderedRange(
        schema: CheckedSchema,
        lower: Bound,
        upper: Bound,
        rule: ValidationRule,
    ) {
        val lowerDecimal = lower.value?.let(SchemaValueSemantics::decimal) ?: return
        val upperDecimal = upper.value?.let(SchemaValueSemantics::decimal) ?: return
        if (lowerDecimal.compareTo(upperDecimal) > 0) {
            schema.results.error(
                rule,
                "${schema.path} '${lower.keyword}' (${lower.value}) cannot be greater than " +
                    "'${upper.keyword}' (${upper.value}) because the generator would emit contradictory constraints.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(schema.node, lower.keyword),
            )
        }
    }
}
