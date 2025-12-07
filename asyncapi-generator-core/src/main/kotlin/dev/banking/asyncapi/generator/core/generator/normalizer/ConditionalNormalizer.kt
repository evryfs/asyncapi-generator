package dev.banking.asyncapi.generator.core.generator.normalizer

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class ConditionalNormalizer : NormalizationStage {

    /**
     * Resolves schemas that contain `if/then/else` keywords by flattening them
     * into a single schema definition. Properties whose types change conditionally
     * will be mapped to a generic `Any` type, and new conditional properties will be merged.
     *
     * @param schemas The map of all discovered and flattened schemas.
     * @return A new map of schemas where conditional logic has been resolved.
     */
    override fun normalize(schemas: Map<String, Schema>): Map<String, Schema> {
        return schemas.mapValues { (_, schema) ->
            if (schema.ifSchema != null || schema.thenSchema != null || schema.elseSchema != null) {
                resolveConditionalSchema(schema)
            } else {
                schema // No conditional logic, return as is.
            }
        }
    }

    private fun resolveConditionalSchema(schema: Schema): Schema {
        val thenSchema = schema.thenSchema.unwrap()
        val elseSchema = schema.elseSchema.unwrap()

        // 1. Get all unique property names from the base, `then`, and `else` schemas.
        val baseProperties = schema.properties ?: emptyMap()
        val thenProperties = thenSchema?.properties ?: emptyMap()
        val elseProperties = elseSchema?.properties ?: emptyMap()
        val allPropertyNames = baseProperties.keys + thenProperties.keys + elseProperties.keys

        val resolvedProperties = mutableMapOf<String, SchemaInterface>()

        // 2. Iterate over each property to resolve conflicts or merge.
        for (propName in allPropertyNames) {
            val baseProp = baseProperties[propName]?.unwrap()
            val thenProp = thenProperties[propName]?.unwrap()
            val elseProp = elseProperties[propName]?.unwrap()

            // 3. Find all unique, non-null types defined for this property across the clauses.
            val typesFound = setOfNotNull(baseProp?.type, thenProp?.type, elseProp?.type)

            if (typesFound.size > 1) {
                // 4. Conflict found: Resolve to 'Any' type by creating a new schemaless property.
                // We keep the description from the base property if it exists, otherwise take the first available.
                resolvedProperties[propName] = SchemaInterface.SchemaInline(
                    Schema(
                        type = null, // This will be mapped to 'Any' by the generator
                        description = baseProp?.description ?: thenProp?.description ?: elseProp?.description
                    )
                )
            } else {
                // 5. No conflict: Merge the property, prioritizing base -> then -> else.
                val finalProp = baseProperties[propName] ?: thenProperties[propName] ?: elseProperties[propName]
                if (finalProp != null) {
                    resolvedProperties[propName] = finalProp
                }
            }
        }

        return schema.copy(
            properties = resolvedProperties,
            ifSchema = null, // Remove conditional keywords as they have been processed.
            thenSchema = null,
            elseSchema = null
        )
    }

    private fun SchemaInterface?.unwrap(): Schema? {
        return (this as? SchemaInterface.SchemaInline)?.schema
    }
}
