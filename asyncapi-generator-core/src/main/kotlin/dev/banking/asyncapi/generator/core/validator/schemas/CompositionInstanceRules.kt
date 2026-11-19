package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.validator.schemas.InstanceEvaluation.Companion.violation

/**
 * Checks values against the Draft 7 keywords that combine schemas: `allOf`, `anyOf`, `oneOf`, `not` and
 * `if`/`then`/`else`.
 *
 * @param evaluator checks the value against each combined schema
 */
internal class CompositionInstanceRules(
    private val evaluator: InstanceEvaluator,
) {
    /** Checks [value] at [path] against the combining keywords of [schema]. */
    fun evaluate(
        schema: Schema,
        value: Any?,
        path: String,
        visited: MutableSet<InstanceVisit>,
    ): InstanceEvaluation {
        var result = InstanceEvaluation()
        schema.allOf?.forEach { child -> result += evaluator.evaluate(child, value, path, visited) }
        schema.anyOf?.let { alternatives -> result += evaluateAnyOf(alternatives, value, path, visited) }
        schema.oneOf?.let { alternatives -> result += evaluateOneOf(alternatives, value, path, visited) }
        schema.not?.let { prohibited -> result += evaluateNot(prohibited, value, path, visited) }
        schema.ifSchema?.let { condition -> result += evaluateCondition(schema, condition, value, path, visited) }
        return result
    }

    private fun evaluateNot(
        prohibited: SchemaInterface,
        value: Any?,
        path: String,
        visited: MutableSet<InstanceVisit>,
    ): InstanceEvaluation {
        val prohibitedResult = evaluator.evaluate(prohibited, value, path, visited.toMutableSet())
        return when {
            prohibitedResult.valid -> violation(path, "matches a schema prohibited by 'not'")
            prohibitedResult.indeterminate ->
                InstanceEvaluation(unsupportedFormats = prohibitedResult.unsupportedFormats)
            else -> InstanceEvaluation()
        }
    }

    private fun evaluateCondition(
        schema: Schema,
        condition: SchemaInterface,
        value: Any?,
        path: String,
        visited: MutableSet<InstanceVisit>,
    ): InstanceEvaluation {
        val conditionResult = evaluator.evaluate(condition, value, path, visited.toMutableSet())
        return when {
            conditionResult.valid ->
                schema.thenSchema?.let { evaluator.evaluate(it, value, path, visited) } ?: InstanceEvaluation()
            conditionResult.indeterminate ->
                InstanceEvaluation(unsupportedFormats = conditionResult.unsupportedFormats)
            else -> schema.elseSchema?.let { evaluator.evaluate(it, value, path, visited) } ?: InstanceEvaluation()
        }
    }

    private fun evaluateAnyOf(
        alternatives: List<SchemaInterface>,
        value: Any?,
        path: String,
        visited: MutableSet<InstanceVisit>,
    ): InstanceEvaluation {
        val evaluations = alternatives.map { evaluator.evaluate(it, value, path, visited.toMutableSet()) }
        if (evaluations.any(InstanceEvaluation::valid)) return InstanceEvaluation()
        val unsupported = evaluations.flatMap(InstanceEvaluation::unsupportedFormats)
        return if (unsupported.isNotEmpty()) {
            InstanceEvaluation(unsupportedFormats = unsupported)
        } else {
            violation(path, "does not match any schema in 'anyOf'")
        }
    }

    private fun evaluateOneOf(
        alternatives: List<SchemaInterface>,
        value: Any?,
        path: String,
        visited: MutableSet<InstanceVisit>,
    ): InstanceEvaluation {
        val evaluations = alternatives.map { evaluator.evaluate(it, value, path, visited.toMutableSet()) }
        val validCount = evaluations.count(InstanceEvaluation::valid)
        val unsupported = evaluations.flatMap(InstanceEvaluation::unsupportedFormats)
        return when {
            validCount > 1 -> violation(path, "must match exactly one schema in 'oneOf', but matched $validCount")
            unsupported.isNotEmpty() -> InstanceEvaluation(unsupportedFormats = unsupported)
            validCount == 1 -> InstanceEvaluation()
            else -> violation(path, "must match exactly one schema in 'oneOf', but matched $validCount")
        }
    }
}
