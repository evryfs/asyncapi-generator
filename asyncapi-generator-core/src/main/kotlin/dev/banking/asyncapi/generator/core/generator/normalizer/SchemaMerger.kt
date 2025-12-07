package dev.banking.asyncapi.generator.core.generator.normalizer

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * A stateless utility class responsible for performing a deep, semantic merge of two schemas.
 * This is used to resolve `allOf` compositions.
 */
class SchemaMerger {

    /**
     * Merges two schemas, with the properties of the 'override' schema taking precedence or being intersected.
     */
    fun merge(base: Schema, override: Schema): Schema {
        // Deep merge properties by recursively merging schemas for each matching property key
        val baseProps = base.properties ?: emptyMap()
        val overrideProps = override.properties ?: emptyMap()
        val allPropKeys = baseProps.keys + overrideProps.keys

        val mergedProperties = allPropKeys.associateWith { key ->
            val baseProp = baseProps[key]
            val overrideProp = overrideProps[key]

            when {
                baseProp != null && overrideProp != null -> {
                    if (baseProp is SchemaInterface.SchemaInline && overrideProp is SchemaInterface.SchemaInline) {
                        SchemaInterface.SchemaInline(merge(baseProp.schema, overrideProp.schema))
                    } else {
                        overrideProp // Fallback: cannot merge complex cases like ref+inline, override wins
                    }
                }
                else -> overrideProp ?: baseProp!!
            }
        }.ifEmpty { null }

        // Create a new schema with merged fields. `override` values take precedence.
        return override.copy(
            title = override.title ?: base.title,
            type = override.type ?: base.type,
            enum = override.enum ?: base.enum,
            format = override.format ?: base.format,
            description = override.description ?: base.description,
            default = override.default ?: base.default,
            const = override.const ?: base.const,
            nullable = override.nullable ?: base.nullable,
            readOnly = override.readOnly ?: base.readOnly,
            writeOnly = override.writeOnly ?: base.writeOnly,

            // For numeric constraints, intersect the ranges (strictest applies)
            minimum = listOfNotNull(base.minimum, override.minimum).maxByOrNull { it.toDouble() },
            maximum = listOfNotNull(base.maximum, override.maximum).minByOrNull { it.toDouble() },

            // For string length, intersect the ranges (strictest applies)
            minLength = listOfNotNull(base.minLength, override.minLength).maxByOrNull { it.toLong() },
            maxLength = listOfNotNull(base.maxLength, override.maxLength).minByOrNull { it.toLong() },

            properties = mergedProperties,
            required = (base.required.orEmpty() + override.required.orEmpty()).distinct(),
            allOf = null
        )
    }
}
