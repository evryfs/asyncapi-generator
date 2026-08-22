package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import java.util.regex.Pattern

/** Validates JSON-compatible instance values against parsed Draft 7 Schema Objects. */
internal class SchemaInstanceValidator(
    private val asyncApiContext: AsyncApiContext,
) {
    data class Violation(val path: String, val message: String)
    data class UnsupportedFormat(val path: String, val schemaFormat: String)
    data class Evaluation(
        val violations: List<Violation> = emptyList(),
        val unsupportedFormats: List<UnsupportedFormat> = emptyList(),
    ) {
        val valid: Boolean get() = violations.isEmpty() && unsupportedFormats.isEmpty()
        val indeterminate: Boolean get() = violations.isEmpty() && unsupportedFormats.isNotEmpty()

        operator fun plus(other: Evaluation): Evaluation =
            Evaluation(violations + other.violations, unsupportedFormats + other.unsupportedFormats)
    }

    fun validate(schema: SchemaInterface, value: Any?, path: String): Evaluation =
        evaluate(schema, value, path, mutableSetOf())

    private fun evaluate(
        schema: Any,
        value: Any?,
        path: String,
        visited: MutableSet<Visit>,
    ): Evaluation {
        val visit = Visit(schema, path)
        if (!visited.add(visit)) return Evaluation()

        return when (schema) {
            is SchemaInterface.SchemaInline -> evaluate(schema.schema, value, path, visited)
            is SchemaInterface.SchemaReference -> evaluateReference(schema.reference, value, path, visited)
            is SchemaInterface.BooleanSchema ->
                if (schema.value) Evaluation() else violation(path, "is rejected by the boolean schema 'false'")
            is SchemaInterface.MultiFormatSchemaInline -> unsupported(path, schema.multiFormatSchema)
            is Schema -> evaluateSchema(schema, value, path, visited)
            is Reference -> evaluateReference(schema, value, path, visited)
            is MultiFormatSchema -> unsupported(path, schema)
            else -> Evaluation()
        }
    }

    private fun evaluateReference(
        reference: Reference,
        value: Any?,
        path: String,
        visited: MutableSet<Visit>,
    ): Evaluation =
        asyncApiContext.modelTracking.findReference(reference)?.let { target ->
            evaluate(target, value, path, visited)
        } ?: Evaluation()

    private fun evaluateSchema(
        schema: Schema,
        value: Any?,
        path: String,
        visited: MutableSet<Visit>,
    ): Evaluation {
        var result = Evaluation()
        if (!SchemaValueSemantics.isCompatible(value, schema.type)) {
            result += violation(path, "does not match declared type '${schema.type}'")
        }
        schema.enum?.takeIf { values -> values.none { SchemaValueSemantics.equal(it, value) } }?.let {
            result += violation(path, "is not one of the schema's enum values")
        }
        if (schema.constSet && !SchemaValueSemantics.equal(schema.const, value)) {
            result += violation(path, "does not equal the schema's const value")
        }

        when (value) {
            is Number -> result += evaluateNumber(schema, value, path)
            is String -> result += evaluateString(schema, value, path)
            is List<*> -> result += evaluateArray(schema, value, path, visited)
            is Map<*, *> -> result += evaluateObject(schema, value, path, visited)
        }

        schema.allOf?.forEach { child -> result += evaluate(child, value, path, visited) }
        schema.anyOf?.let { alternatives -> result += evaluateAnyOf(alternatives, value, path, visited) }
        schema.oneOf?.let { alternatives -> result += evaluateOneOf(alternatives, value, path, visited) }
        schema.not?.let { prohibited ->
            val prohibitedResult = evaluate(prohibited, value, path, visited.toMutableSet())
            result += when {
                prohibitedResult.valid -> violation(path, "matches a schema prohibited by 'not'")
                prohibitedResult.indeterminate -> Evaluation(unsupportedFormats = prohibitedResult.unsupportedFormats)
                else -> Evaluation()
            }
        }
        schema.ifSchema?.let { condition ->
            val conditionResult = evaluate(condition, value, path, visited.toMutableSet())
            result += when {
                conditionResult.valid -> schema.thenSchema?.let { evaluate(it, value, path, visited) } ?: Evaluation()
                conditionResult.indeterminate -> Evaluation(unsupportedFormats = conditionResult.unsupportedFormats)
                else -> schema.elseSchema?.let { evaluate(it, value, path, visited) } ?: Evaluation()
            }
        }
        return result
    }

    private fun evaluateNumber(schema: Schema, value: Number, path: String): Evaluation {
        val number = SchemaValueSemantics.decimal(value) ?: return Evaluation()
        var result = Evaluation()
        schema.minimum?.let(SchemaValueSemantics::decimal)?.takeIf { number < it }?.let {
            result += violation(path, "is lower than the inclusive minimum $it")
        }
        schema.maximum?.let(SchemaValueSemantics::decimal)?.takeIf { number > it }?.let {
            result += violation(path, "is greater than the inclusive maximum $it")
        }
        schema.exclusiveMinimum?.let(SchemaValueSemantics::decimal)?.takeIf { number <= it }?.let {
            result += violation(path, "is not greater than the exclusive minimum $it")
        }
        schema.exclusiveMaximum?.let(SchemaValueSemantics::decimal)?.takeIf { number >= it }?.let {
            result += violation(path, "is not lower than the exclusive maximum $it")
        }
        schema.multipleOf?.let(SchemaValueSemantics::decimal)?.takeIf { divisor ->
            divisor.signum() > 0 && number.remainder(divisor).compareTo(java.math.BigDecimal.ZERO) != 0
        }?.let { divisor ->
            result += violation(path, "is not a multiple of $divisor")
        }
        return result
    }

    private fun evaluateString(schema: Schema, value: String, path: String): Evaluation {
        val length = value.codePointCount(0, value.length).toBigDecimal()
        var result = Evaluation()
        schema.minLength?.let(SchemaValueSemantics::decimal)?.takeIf { length < it }?.let {
            result += violation(path, "is shorter than minLength $it")
        }
        schema.maxLength?.let(SchemaValueSemantics::decimal)?.takeIf { length > it }?.let {
            result += violation(path, "is longer than maxLength $it")
        }
        schema.pattern?.let { regex ->
            val matches = runCatching { Pattern.compile(regex).matcher(value).find() }.getOrDefault(true)
            if (!matches) result += violation(path, "does not match pattern '$regex'")
        }
        return result
    }

    private fun evaluateArray(
        schema: Schema,
        value: List<*>,
        path: String,
        visited: MutableSet<Visit>,
    ): Evaluation {
        val size = value.size.toBigDecimal()
        var result = Evaluation()
        schema.minItems?.let(SchemaValueSemantics::decimal)?.takeIf { size < it }?.let {
            result += violation(path, "contains fewer items than minItems $it")
        }
        schema.maxItems?.let(SchemaValueSemantics::decimal)?.takeIf { size > it }?.let {
            result += violation(path, "contains more items than maxItems $it")
        }
        if (schema.uniqueItems == true && SchemaValueSemantics.hasDuplicates(value)) {
            result += violation(path, "contains duplicate items while uniqueItems is true")
        }

        schema.items?.let { itemSchema ->
            value.forEachIndexed { index, item ->
                result += evaluate(itemSchema, item, "$path[$index]", visited)
            }
        }
        schema.tupleItems?.let { tuple ->
            value.forEachIndexed { index, item ->
                val itemSchema = tuple.getOrNull(index) ?: schema.additionalItems ?: return@forEachIndexed
                result += evaluate(itemSchema, item, "$path[$index]", visited)
            }
        }
        schema.contains?.let { containedSchema ->
            val evaluations = value.mapIndexed { index, item ->
                evaluate(containedSchema, item, "$path[$index]", visited.toMutableSet())
            }
            if (evaluations.none(Evaluation::valid)) {
                val unsupported = evaluations.flatMap(Evaluation::unsupportedFormats)
                result += if (unsupported.isNotEmpty()) {
                    Evaluation(unsupportedFormats = unsupported)
                } else {
                    violation(path, "does not contain an item matching the 'contains' schema")
                }
            }
        }
        return result
    }

    private fun evaluateObject(
        schema: Schema,
        value: Map<*, *>,
        path: String,
        visited: MutableSet<Visit>,
    ): Evaluation {
        val properties = value.entries.filter { it.key is String }.associate { it.key as String to it.value }
        val size = properties.size.toBigDecimal()
        var result = Evaluation()
        schema.minProperties?.let(SchemaValueSemantics::decimal)?.takeIf { size < it }?.let {
            result += violation(path, "contains fewer properties than minProperties $it")
        }
        schema.maxProperties?.let(SchemaValueSemantics::decimal)?.takeIf { size > it }?.let {
            result += violation(path, "contains more properties than maxProperties $it")
        }
        schema.required.orEmpty().filterNot(properties::containsKey).forEach { required ->
            result += violation(path, "is missing required property '$required'")
        }

        val propertyPatterns = schema.patternProperties.orEmpty().mapNotNull { (regex, propertySchema) ->
            runCatching { Pattern.compile(regex) }.getOrNull()?.let { it to propertySchema }
        }
        properties.forEach { (name, propertyValue) ->
            val propertyPath = "$path.$name"
            schema.properties?.get(name)?.let { propertySchema ->
                result += evaluate(propertySchema, propertyValue, propertyPath, visited)
            }
            val matchedPatterns = propertyPatterns.filter { (pattern, _) -> pattern.matcher(name).find() }
            matchedPatterns.forEach { (_, propertySchema) ->
                result += evaluate(propertySchema, propertyValue, propertyPath, visited)
            }
            if (schema.properties?.containsKey(name) != true && matchedPatterns.isEmpty()) {
                schema.additionalProperties?.let { additional ->
                    result += evaluate(additional, propertyValue, propertyPath, visited)
                }
            }
            schema.propertyNames?.let { propertyNameSchema ->
                result += evaluate(propertyNameSchema, name, propertyPath, visited)
            }
        }

        schema.dependencies.orEmpty().forEach { (property, dependency) ->
            if (property !in properties) return@forEach
            when (dependency) {
                is List<*> -> dependency.filterIsInstance<String>().filterNot(properties::containsKey).forEach { name ->
                    result += violation(path, "property '$property' requires property '$name'")
                }
                is SchemaInterface -> result += evaluate(dependency, value, path, visited)
            }
        }
        return result
    }

    private fun evaluateAnyOf(
        alternatives: List<SchemaInterface>,
        value: Any?,
        path: String,
        visited: MutableSet<Visit>,
    ): Evaluation {
        val evaluations = alternatives.map { evaluate(it, value, path, visited.toMutableSet()) }
        if (evaluations.any(Evaluation::valid)) return Evaluation()
        val unsupported = evaluations.flatMap(Evaluation::unsupportedFormats)
        return if (unsupported.isNotEmpty()) {
            Evaluation(unsupportedFormats = unsupported)
        } else {
            violation(path, "does not match any schema in 'anyOf'")
        }
    }

    private fun evaluateOneOf(
        alternatives: List<SchemaInterface>,
        value: Any?,
        path: String,
        visited: MutableSet<Visit>,
    ): Evaluation {
        val evaluations = alternatives.map { evaluate(it, value, path, visited.toMutableSet()) }
        val validCount = evaluations.count(Evaluation::valid)
        val unsupported = evaluations.flatMap(Evaluation::unsupportedFormats)
        return when {
            validCount > 1 -> violation(path, "must match exactly one schema in 'oneOf', but matched $validCount")
            unsupported.isNotEmpty() -> Evaluation(unsupportedFormats = unsupported)
            validCount == 1 -> Evaluation()
            else -> violation(path, "must match exactly one schema in 'oneOf', but matched $validCount")
        }
    }

    private fun violation(path: String, message: String) = Evaluation(listOf(Violation(path, message)))

    private fun unsupported(path: String, schema: MultiFormatSchema) =
        Evaluation(unsupportedFormats = listOf(UnsupportedFormat(path, schema.schemaFormat)))

    private class Visit(private val schema: Any, private val path: String) {
        override fun equals(other: Any?): Boolean = other is Visit && schema === other.schema && path == other.path
        override fun hashCode(): Int = 31 * System.identityHashCode(schema) + path.hashCode()
    }
}
