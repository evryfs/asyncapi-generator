package dev.banking.asyncapi.generator.core.generator.normalizer

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class CompositionNormalizer : NormalizationStage {

    private val schemaMerger = SchemaMerger()

    override fun normalize(schemas: Map<String, Schema>): Map<String, Schema> {
        return schemas.mapValues { (name, schema) ->
            resolveSchemaRecursive(schema, name, schemas, mutableSetOf())
        }
    }

    private fun resolveSchemaRecursive(
        schema: Schema,
        schemaName: String,
        allSchemas: Map<String, Schema>,
        visited: MutableSet<String>,
    ): Schema {
        if (schemaName in visited) return schema

        var currentSchema = schema
        visited.add(schemaName)

        currentSchema.properties?.let { properties ->
            val newProperties = properties.mapValues { (propName, propSchema) ->
                if (propSchema is SchemaInterface.SchemaInline) {
                    SchemaInterface.SchemaInline(
                        resolveSchemaRecursive(
                            propSchema.schema,
                            "$schemaName.$propName",
                            allSchemas,
                            visited
                        )
                    )
                } else {
                    propSchema
                }
            }
            currentSchema = currentSchema.copy(properties = newProperties)
        }

        if (currentSchema.allOf.isNullOrEmpty()) {
            visited.remove(schemaName)
            return currentSchema
        }

        var mergedSchema = currentSchema
        var titleFromRef: String? = null

        currentSchema.allOf.forEach { subSchemaInterface ->
            if (subSchemaInterface is SchemaInterface.SchemaReference) {
                titleFromRef = subSchemaInterface.reference.ref.substringAfterLast('/')
            }

            val parentSchema = resolveSubSchema(subSchemaInterface, schemaName, allSchemas, visited)
            if (parentSchema != null) {
                mergedSchema = schemaMerger.merge(parentSchema, mergedSchema)
            }
        }

        visited.remove(schemaName)

        return mergedSchema.copy(
            allOf = null,
            title = mergedSchema.title ?: titleFromRef
        )
    }

    private fun resolveSubSchema(
        subSchemaInterface: SchemaInterface,
        parentName: String,
        allSchemas: Map<String, Schema>,
        visited: MutableSet<String>,
    ): Schema? {
        return when (subSchemaInterface) {
            is SchemaInterface.SchemaInline -> {
                resolveSchemaRecursive(subSchemaInterface.schema, "inline_in_${parentName}", allSchemas, visited)
            }

            is SchemaInterface.SchemaReference -> {
                val refName = subSchemaInterface.reference.ref.substringAfterLast('/')
                val referencedSchema = (subSchemaInterface.reference.model as? Schema)
                    ?: allSchemas[refName]
                    // TODO - better error handling
                    ?: throw IllegalArgumentException(
                        "CompositionProcessor: Unresolved reference in allOf for " +
                            "schema '$parentName': ${subSchemaInterface.reference.ref}"
                    )
                resolveSchemaRecursive(referencedSchema, refName, allSchemas, visited)
            }

            else -> null
        }
    }
}
