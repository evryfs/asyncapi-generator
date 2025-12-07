package com.tietoevry.banking.asyncapi.generator.core.generator.analyzer

import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Discovers schemas embedded within SchemaReference objects and adds them to the main schema map.
 */
class ReferenceAnalyzer : AnalysisStage<Map<String, Schema>> {

    override fun analyze(schemas: Map<String, Schema>): Map<String, Schema> {
        val discoveredSchemas = schemas.toMutableMap()
        val namesToProcess = discoveredSchemas.keys.toMutableList()
        val processedSet = mutableSetOf<String>()

        while (namesToProcess.isNotEmpty()) {
            val schemaName = namesToProcess.removeFirst()
            if (schemaName in processedSet) continue
            processedSet.add(schemaName)

            val originalSchema = discoveredSchemas[schemaName] ?: continue
            discoverReferencesInSchema(originalSchema, discoveredSchemas, namesToProcess)
        }
        return discoveredSchemas
    }

    private fun discoverReferencesInSchema(
        schema: Schema,
        discoveredSchemas: MutableMap<String, Schema>,
        namesToProcess: MutableList<String>
    ) {
        // Look in properties
        schema.properties?.values?.forEach { processSubSchema(it, discoveredSchemas, namesToProcess) }

        // Look in array items
        schema.items?.let { processSubSchema(it, discoveredSchemas, namesToProcess) }

        // --- NEW: Look in compositions (oneOf, anyOf, allOf) ---
        schema.oneOf?.forEach { processSubSchema(it, discoveredSchemas, namesToProcess) }
        schema.anyOf?.forEach { processSubSchema(it, discoveredSchemas, namesToProcess) }
        schema.allOf?.forEach { processSubSchema(it, discoveredSchemas, namesToProcess) }
    }

    private fun processSubSchema(
        schemaInterface: SchemaInterface,
        discoveredSchemas: MutableMap<String, Schema>,
        namesToProcess: MutableList<String>
    ) {
        when (schemaInterface) {
            is SchemaInterface.SchemaReference -> {
                val refModel = schemaInterface.reference.model
                // Ensure refModel is a Schema before adding
                if (refModel != null && refModel is Schema) {
                    val refName = MapperUtil.toPascalCase(schemaInterface.reference.ref.substringAfterLast('/'))
                    if (!discoveredSchemas.containsKey(refName)) {
                        discoveredSchemas[refName] = refModel
                        namesToProcess.add(refName) // Add to queue to process its children, too
                    }
                }
            }
            is SchemaInterface.SchemaInline -> {
                // If it's an inline schema, recurse into its contents to find references within it
                discoverReferencesInSchema(schemaInterface.schema, discoveredSchemas, namesToProcess)
            }
            else -> { }
        }
    }
}
