package dev.banking.asyncapi.generator.core.bundler.schemas

import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import java.util.Collections
import java.util.IdentityHashMap

/** Identifies schema models that participate in a reference cycle. */
internal class SchemaRecursionAnalyzer {

    private val results = IdentityHashMap<Any, Boolean>()

    fun isRecursive(model: Any): Boolean =
        results[model] ?: reachesModel(
            target = model,
            model = model,
            visited = identitySet<Any>().apply { add(model) },
        ).also { recursive -> results[model] = recursive }

    private fun reachesModel(
        target: Any,
        model: Any,
        visited: MutableSet<Any>,
    ): Boolean =
        when (model) {
            is Schema -> model.nestedSchemas().any { nested -> reaches(target, nested, visited) }
            is MultiFormatSchema -> false
            else -> false
        }

    private fun reaches(
        target: Any,
        schema: SchemaInterface,
        visited: MutableSet<Any>,
    ): Boolean =
        when (schema) {
            is SchemaInterface.SchemaInline ->
                visited.add(schema.schema) && reachesModel(target, schema.schema, visited)

            is SchemaInterface.SchemaReference -> {
                val referencedModel = schema.reference.requireModel<Any>()
                referencedModel === target ||
                    (visited.add(referencedModel) && reachesModel(target, referencedModel, visited))
            }

            is SchemaInterface.MultiFormatSchemaInline,
            is SchemaInterface.BooleanSchema,
            -> false
        }

    private fun Schema.nestedSchemas(): Sequence<SchemaInterface> = sequence {
        listOfNotNull(
            items,
            additionalItems,
            contains,
            additionalProperties,
            propertyNames,
            not,
            ifSchema,
            thenSchema,
            elseSchema,
        ).forEach { yield(it) }
        properties.orEmpty().values.forEach { yield(it) }
        patternProperties.orEmpty().values.forEach { yield(it) }
        definitions.orEmpty().values.forEach { yield(it) }
        dependencies.orEmpty().values.filterIsInstance<SchemaInterface>().forEach { yield(it) }
        allOf.orEmpty().forEach { yield(it) }
        anyOf.orEmpty().forEach { yield(it) }
        oneOf.orEmpty().forEach { yield(it) }
    }

    private fun <T : Any> identitySet(): MutableSet<T> =
        Collections.newSetFromMap(IdentityHashMap())
}
