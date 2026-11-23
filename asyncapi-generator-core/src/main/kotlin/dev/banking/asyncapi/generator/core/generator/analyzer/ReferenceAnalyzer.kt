package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.generator.schema.generatedSchemaName
import dev.banking.asyncapi.generator.core.generator.schema.linkedSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Adds every schema that [analyze]'s input references, directly or through
 * other referenced schemas, under its generated name.
 *
 * A name is queued only when it is new, so each schema is searched once.
 */
internal class ReferenceAnalyzer {
    /** [schemas] plus the schemas they reference; a name already present keeps its schema. */
    fun analyze(schemas: Map<String, Schema>): Map<String, Schema> {
        val discoveredSchemas = schemas.toMutableMap()
        val namesToProcess = discoveredSchemas.keys.toMutableList()
        while (namesToProcess.isNotEmpty()) {
            val schema = discoveredSchemas.getValue(namesToProcess.removeFirst())
            discoverReferencesInSchema(schema, discoveredSchemas, namesToProcess)
        }
        return discoveredSchemas
    }

    private fun discoverReferencesInSchema(
        schema: Schema,
        discoveredSchemas: MutableMap<String, Schema>,
        namesToProcess: MutableList<String>,
    ) {
        schema.properties?.values?.forEach { processSubSchema(it, discoveredSchemas, namesToProcess) }
        schema.items?.let { processSubSchema(it, discoveredSchemas, namesToProcess) }
        schema.additionalProperties?.let { processSubSchema(it, discoveredSchemas, namesToProcess) }
        schema.oneOf?.forEach { processSubSchema(it, discoveredSchemas, namesToProcess) }
        schema.anyOf?.forEach { processSubSchema(it, discoveredSchemas, namesToProcess) }
        schema.allOf?.forEach { processSubSchema(it, discoveredSchemas, namesToProcess) }
    }

    private fun processSubSchema(
        schemaInterface: SchemaInterface,
        discoveredSchemas: MutableMap<String, Schema>,
        namesToProcess: MutableList<String>,
    ) {
        when (schemaInterface) {
            is SchemaInterface.SchemaReference -> {
                val referencedSchema = schemaInterface.linkedSchema() ?: return
                val refName = schemaInterface.reference.generatedSchemaName()
                if (!discoveredSchemas.containsKey(refName)) {
                    discoveredSchemas[refName] = referencedSchema
                    namesToProcess.add(refName)
                }
            }
            is SchemaInterface.SchemaInline ->
                discoverReferencesInSchema(schemaInterface.schema, discoveredSchemas, namesToProcess)
            else -> Unit
        }
    }
}
