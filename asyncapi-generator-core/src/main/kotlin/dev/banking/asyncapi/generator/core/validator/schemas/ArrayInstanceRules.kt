package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.validator.schemas.InstanceEvaluation.Companion.violation

/**
 * Checks arrays against the Draft 7 keywords that constrain them and their items.
 *
 * @param evaluator checks each item against its schema
 */
internal class ArrayInstanceRules(
    private val evaluator: InstanceEvaluator,
) {
    /** Checks the array [value] at [path] against the array keywords of [schema]. */
    fun evaluate(
        schema: Schema,
        value: List<*>,
        path: String,
        visited: MutableSet<InstanceVisit>,
    ): InstanceEvaluation =
        evaluateSize(schema, value, path) +
            evaluateItems(schema, value, path, visited) +
            evaluateTupleItems(schema, value, path, visited) +
            evaluateContains(schema, value, path, visited)

    private fun evaluateSize(
        schema: Schema,
        value: List<*>,
        path: String,
    ): InstanceEvaluation {
        val size = value.size.toBigDecimal()
        var result = InstanceEvaluation()
        schema.minItems?.let(SchemaValueSemantics::decimal)?.takeIf { size < it }?.let {
            result += violation(path, "contains fewer items than minItems $it")
        }
        schema.maxItems?.let(SchemaValueSemantics::decimal)?.takeIf { size > it }?.let {
            result += violation(path, "contains more items than maxItems $it")
        }
        if (schema.uniqueItems == true && SchemaValueSemantics.hasDuplicates(value)) {
            result += violation(path, "contains duplicate items while uniqueItems is true")
        }
        return result
    }

    private fun evaluateItems(
        schema: Schema,
        value: List<*>,
        path: String,
        visited: MutableSet<InstanceVisit>,
    ): InstanceEvaluation {
        var result = InstanceEvaluation()
        schema.items?.let { itemSchema ->
            value.forEachIndexed { index, item ->
                result += evaluator.evaluate(itemSchema, item, "$path[$index]", visited)
            }
        }
        return result
    }

    private fun evaluateTupleItems(
        schema: Schema,
        value: List<*>,
        path: String,
        visited: MutableSet<InstanceVisit>,
    ): InstanceEvaluation {
        var result = InstanceEvaluation()
        schema.tupleItems?.let { tuple ->
            value.forEachIndexed { index, item ->
                val itemSchema = tuple.getOrNull(index) ?: schema.additionalItems ?: return@forEachIndexed
                result += evaluator.evaluate(itemSchema, item, "$path[$index]", visited)
            }
        }
        return result
    }

    private fun evaluateContains(
        schema: Schema,
        value: List<*>,
        path: String,
        visited: MutableSet<InstanceVisit>,
    ): InstanceEvaluation {
        val containedSchema = schema.contains ?: return InstanceEvaluation()
        val evaluations =
            value.mapIndexed { index, item ->
                evaluator.evaluate(containedSchema, item, "$path[$index]", visited.toMutableSet())
            }
        val unsupported = evaluations.flatMap(InstanceEvaluation::unsupportedFormats)
        return when {
            evaluations.any(InstanceEvaluation::valid) -> InstanceEvaluation()
            unsupported.isNotEmpty() -> InstanceEvaluation(unsupportedFormats = unsupported)
            else -> violation(path, "does not contain an item matching the 'contains' schema")
        }
    }
}
