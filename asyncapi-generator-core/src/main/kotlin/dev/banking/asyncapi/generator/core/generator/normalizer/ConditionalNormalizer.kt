package dev.banking.asyncapi.generator.core.generator.normalizer

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class ConditionalNormalizer : NormalizationStage {

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

        val baseProperties = schema.properties ?: emptyMap()
        val thenProperties = thenSchema?.properties ?: emptyMap()
        val elseProperties = elseSchema?.properties ?: emptyMap()
        val allPropertyNames = baseProperties.keys + thenProperties.keys + elseProperties.keys

        val resolvedProperties = mutableMapOf<String, SchemaInterface>()

        for (propName in allPropertyNames) {
            val baseProp = baseProperties[propName]?.unwrap()
            val thenProp = thenProperties[propName]?.unwrap()
            val elseProp = elseProperties[propName]?.unwrap()

            val typesFound = setOfNotNull(baseProp?.type, thenProp?.type, elseProp?.type)

            if (typesFound.size > 1) {
                resolvedProperties[propName] = SchemaInterface.SchemaInline(
                    Schema(
                        type = null,
                        description = baseProp?.description ?: thenProp?.description ?: elseProp?.description
                    )
                )
            } else {
                val finalProp = baseProperties[propName] ?: thenProperties[propName] ?: elseProperties[propName]
                if (finalProp != null) {
                    resolvedProperties[propName] = finalProp
                }
            }
        }

        return schema.copy(
            properties = resolvedProperties,
            ifSchema = null,
            thenSchema = null,
            elseSchema = null
        )
    }

    private fun SchemaInterface?.unwrap(): Schema? {
        return (this as? SchemaInterface.SchemaInline)?.schema
    }
}
