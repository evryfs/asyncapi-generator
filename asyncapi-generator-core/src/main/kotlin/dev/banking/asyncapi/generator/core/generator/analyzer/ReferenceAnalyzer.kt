package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class ReferenceAnalyzer {

    fun analyze(schemas: Map<String, Schema>): Map<String, Schema> {
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
                val referencedSchema = schemaInterface.reference.model as? Schema ?: return
                val refName = MapperUtil.toPascalCase(schemaInterface.reference.ref.substringAfterLast('/'))
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
