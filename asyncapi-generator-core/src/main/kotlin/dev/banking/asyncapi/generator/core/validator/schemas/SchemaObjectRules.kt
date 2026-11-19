package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DEPENDENCY_ARRAY_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DISCRIMINATOR_PROPERTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DISCRIMINATOR_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_REQUIRED_UNDECLARED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_REQUIRED_UNIQUE

/**
 * Validates the keywords that describe the properties of an object schema: `required`, `dependencies` and
 * `discriminator`.
 *
 * @param asyncApiContext the load the schemas belong to
 */
internal class SchemaObjectRules(
    private val asyncApiContext: AsyncApiContext,
) {
    /**
     * Reports a `required` list that repeats a name or names properties no declaration covers.
     *
     * @param schema the schema whose `required` list is checked
     * @param applicableDeclarations the property names declared by the schema and the schemas it composes
     */
    fun checkRequired(
        schema: CheckedSchema,
        applicableDeclarations: Set<String>,
    ) {
        val node = schema.node
        val required = node.required ?: return
        val location = asyncApiContext.modelTracking.getSourceLocation(node, node::required)
        if (required.distinct().size != required.size) {
            schema.results.error(
                SCHEMA_REQUIRED_UNIQUE,
                "${schema.path} 'required' contains duplicate property names.",
                sourceLocation = location,
            )
        }
        val missing = required.filter { it !in applicableDeclarations }
        if (missing.isNotEmpty()) {
            schema.results.warn(
                SCHEMA_REQUIRED_UNDECLARED,
                "${schema.path} lists required properties $missing that are not declared in this object or its " +
                    "'allOf' composition. Define them explicitly, otherwise generation may fail.",
                sourceLocation = location,
            )
        }
    }

    /** Reports a property dependency list that repeats a property name. */
    fun checkDependencies(schema: CheckedSchema) {
        schema.node.dependencies?.forEach { (propertyName, dependency) ->
            val propertyNames = dependency as? List<*> ?: return@forEach
            val duplicateIndex =
                propertyNames.indices.firstOrNull { index ->
                    propertyNames.subList(0, index).contains(propertyNames[index])
                }
            if (duplicateIndex != null) {
                schema.results.error(
                    SCHEMA_DEPENDENCY_ARRAY_UNIQUE,
                    "${schema.path} dependency '$propertyName' contains duplicate property names.",
                    sourceLocation =
                        asyncApiContext.modelTracking.getElementLocation(dependency, duplicateIndex)
                            ?: asyncApiContext.modelTracking.getSourceLocation(dependency),
                )
            }
        }
    }

    /** Reports a `discriminator` property that is not required or not declared in `properties`. */
    fun checkDiscriminator(schema: CheckedSchema) {
        val node = schema.node
        val discriminator = node.discriminator ?: return
        if (discriminator !in node.required.orEmpty()) {
            schema.results.error(
                SCHEMA_DISCRIMINATOR_REQUIRED,
                "${schema.path} discriminator property '$discriminator' must be listed in 'required'.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::discriminator),
            )
        }
        if (discriminator !in node.properties.orEmpty()) {
            schema.results.error(
                SCHEMA_DISCRIMINATOR_PROPERTY,
                "${schema.path} discriminator property '$discriminator' must exist in 'properties'.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::discriminator),
            )
        }
    }
}
