package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ARRAY_SIZE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ARRAY_SIZE_RANGE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_MULTIPLE_OF
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_NUMERIC_RANGE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_OBJECT_SIZE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_OBJECT_SIZE_RANGE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_STRING_LENGTH
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_STRING_LENGTH_RANGE
import dev.banking.asyncapi.generator.core.validator.schemas.SchemaBounds.Bound

/**
 * Validates the numeric limits a Schema Object declares for numbers, strings, arrays and objects.
 *
 * @param asyncApiContext the load the schemas belong to
 */
internal class SchemaConstraintRules(
    private val asyncApiContext: AsyncApiContext,
) {
    private val bounds = SchemaBounds(asyncApiContext)

    /** Reports contradictory `minimum`/`maximum` limits and a `multipleOf` that is not positive. */
    fun checkNumeric(schema: CheckedSchema) {
        val node = schema.node
        bounds.orderedRange(
            schema,
            Bound("minimum", node.minimum),
            Bound("maximum", node.maximum),
            SCHEMA_NUMERIC_RANGE,
        )
        bounds.orderedRange(
            schema,
            Bound("exclusiveMinimum", node.exclusiveMinimum),
            Bound("exclusiveMaximum", node.exclusiveMaximum),
            SCHEMA_NUMERIC_RANGE,
        )
        node.multipleOf?.let {
            if (SchemaValueSemantics.decimal(it)?.signum() != 1) {
                schema.results.error(
                    SCHEMA_MULTIPLE_OF,
                    "${schema.path} 'multipleOf' must be greater than zero.",
                    sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::multipleOf),
                )
            }
        }
    }

    /** Reports invalid or contradictory `minLength`/`maxLength` limits. */
    fun checkStringLength(schema: CheckedSchema) =
        checkSize(
            schema,
            Bound("minLength", schema.node.minLength) to Bound("maxLength", schema.node.maxLength),
            SCHEMA_STRING_LENGTH to SCHEMA_STRING_LENGTH_RANGE,
        )

    /** Reports invalid or contradictory `minItems`/`maxItems` limits. */
    fun checkArraySize(schema: CheckedSchema) =
        checkSize(
            schema,
            Bound("minItems", schema.node.minItems) to Bound("maxItems", schema.node.maxItems),
            SCHEMA_ARRAY_SIZE to SCHEMA_ARRAY_SIZE_RANGE,
        )

    /** Reports invalid or contradictory `minProperties`/`maxProperties` limits. */
    fun checkObjectSize(schema: CheckedSchema) =
        checkSize(
            schema,
            Bound("minProperties", schema.node.minProperties) to Bound("maxProperties", schema.node.maxProperties),
            SCHEMA_OBJECT_SIZE to SCHEMA_OBJECT_SIZE_RANGE,
        )

    private fun checkSize(
        schema: CheckedSchema,
        limits: Pair<Bound, Bound>,
        rules: Pair<ValidationRule, ValidationRule>,
    ) {
        val (lower, upper) = limits
        val (valueRule, rangeRule) = rules
        val minimum = bounds.nonNegativeInteger(schema, lower, valueRule)
        val maximum = bounds.nonNegativeInteger(schema, upper, valueRule)
        bounds.orderedRange(schema, Bound(lower.keyword, minimum), Bound(upper.keyword, maximum), rangeRule)
    }
}
