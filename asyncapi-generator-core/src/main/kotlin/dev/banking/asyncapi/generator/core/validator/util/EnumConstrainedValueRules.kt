package dev.banking.asyncapi.generator.core.validator.util

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import dev.banking.asyncapi.generator.core.parser.tracking.ModelTracking
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath

/**
 * The rules of a value constrained by `enum`, with a `default` and `examples`, as Parameter and Server Variable
 * objects define it.
 *
 * @param uniqueRule reported when `enum` has duplicate values
 * @param defaultRule reported when `default` is not one of the `enum` values
 * @param examplesRule reported when an example is not one of the `enum` values
 */
internal class EnumConstrainedValueRules(
    private val uniqueRule: ValidationRule,
    private val defaultRule: ValidationRule,
    private val examplesRule: ValidationRule,
) {
    /**
     * Checks the `enum`, `default` and `examples` members of [model].
     *
     * @param model the object the members belong to
     * @param value the members' values
     * @param path the object as named in findings
     * @param results the collector findings are reported to
     * @param modelTracking where the members are written
     */
    fun validate(
        model: Any,
        value: EnumConstrainedValue,
        path: ValidationPath,
        results: ValidationCollector,
        modelTracking: ModelTracking,
    ) {
        val enum = value.enum ?: return
        if (enum.distinct().size != enum.size) {
            results.error(
                uniqueRule,
                "$path 'enum' contains duplicate values.",
                modelTracking.getSourceLocation(model, "enum"),
            )
        }
        value.default?.takeIf { it !in enum }?.let { default ->
            results.warn(
                defaultRule,
                "$path 'default' ('$default') is not one of the 'enum' values.",
                modelTracking.getSourceLocation(model, "default"),
            )
        }
        if (value.examples.orEmpty().any { it !in enum }) {
            results.warn(
                examplesRule,
                "$path 'examples' contain values that are not in 'enum'.",
                modelTracking.getSourceLocation(model, "examples"),
            )
        }
    }
}

/**
 * The `enum`, `default` and `examples` members of an object.
 *
 * @property enum the allowed values
 * @property default the default value
 * @property examples the example values
 */
internal data class EnumConstrainedValue(
    val enum: List<String>?,
    val default: String?,
    val examples: List<String>?,
)
