package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.validator.schemas.InstanceEvaluation.Companion.violation
import java.util.regex.Pattern

/**
 * Checks objects against the Draft 7 keywords that constrain them and their properties.
 *
 * @param evaluator checks each property value against its schema
 */
internal class ObjectInstanceRules(
    private val evaluator: InstanceEvaluator,
) {
    /** Checks the object [value] at [path] against the object keywords of [schema]. */
    fun evaluate(
        schema: Schema,
        value: Map<*, *>,
        path: String,
        visited: MutableSet<InstanceVisit>,
    ): InstanceEvaluation {
        val properties = value.entries.filter { it.key is String }.associate { it.key as String to it.value }
        return evaluateSize(schema, properties, path) +
            evaluateRequired(schema, properties, path) +
            evaluateProperties(schema, properties, path, visited) +
            evaluateDependencies(schema, value, properties, path, visited)
    }

    private fun evaluateSize(
        schema: Schema,
        properties: Map<String, Any?>,
        path: String,
    ): InstanceEvaluation {
        val size = properties.size.toBigDecimal()
        var result = InstanceEvaluation()
        schema.minProperties?.let(SchemaValueSemantics::decimal)?.takeIf { size < it }?.let {
            result += violation(path, "contains fewer properties than minProperties $it")
        }
        schema.maxProperties?.let(SchemaValueSemantics::decimal)?.takeIf { size > it }?.let {
            result += violation(path, "contains more properties than maxProperties $it")
        }
        return result
    }

    private fun evaluateRequired(
        schema: Schema,
        properties: Map<String, Any?>,
        path: String,
    ): InstanceEvaluation =
        schema.required.orEmpty().filterNot(properties::containsKey).fold(InstanceEvaluation()) { result, required ->
            result + violation(path, "is missing required property '$required'")
        }

    private fun evaluateProperties(
        schema: Schema,
        properties: Map<String, Any?>,
        path: String,
        visited: MutableSet<InstanceVisit>,
    ): InstanceEvaluation {
        val propertyPatterns =
            schema.patternProperties.orEmpty().mapNotNull { (regex, propertySchema) ->
                runCatching { Pattern.compile(regex) }.getOrNull()?.let { it to propertySchema }
            }
        return properties.entries.fold(InstanceEvaluation()) { result, property ->
            result + evaluateProperty(schema, propertyPatterns, property, path, visited)
        }
    }

    private fun evaluateProperty(
        schema: Schema,
        propertyPatterns: List<Pair<Pattern, SchemaInterface>>,
        property: Map.Entry<String, Any?>,
        path: String,
        visited: MutableSet<InstanceVisit>,
    ): InstanceEvaluation {
        val (name, propertyValue) = property
        val propertyPath = "$path.$name"
        var result = InstanceEvaluation()
        schema.properties?.get(name)?.let { propertySchema ->
            result += evaluator.evaluate(propertySchema, propertyValue, propertyPath, visited)
        }
        val matchedPatterns = propertyPatterns.filter { (pattern, _) -> pattern.matcher(name).find() }
        matchedPatterns.forEach { (_, propertySchema) ->
            result += evaluator.evaluate(propertySchema, propertyValue, propertyPath, visited)
        }
        if (schema.properties?.containsKey(name) != true && matchedPatterns.isEmpty()) {
            schema.additionalProperties?.let { additional ->
                result += evaluator.evaluate(additional, propertyValue, propertyPath, visited)
            }
        }
        schema.propertyNames?.let { propertyNameSchema ->
            result += evaluator.evaluate(propertyNameSchema, name, propertyPath, visited)
        }
        return result
    }

    private fun evaluateDependencies(
        schema: Schema,
        value: Map<*, *>,
        properties: Map<String, Any?>,
        path: String,
        visited: MutableSet<InstanceVisit>,
    ): InstanceEvaluation {
        var result = InstanceEvaluation()
        schema.dependencies.orEmpty().forEach { (property, dependency) ->
            if (property !in properties) return@forEach
            when (dependency) {
                is List<*> ->
                    dependency.filterIsInstance<String>().filterNot(properties::containsKey).forEach { name ->
                        result += violation(path, "property '$property' requires property '$name'")
                    }
                is SchemaInterface -> result += evaluator.evaluate(dependency, value, path, visited)
            }
        }
        return result
    }
}
