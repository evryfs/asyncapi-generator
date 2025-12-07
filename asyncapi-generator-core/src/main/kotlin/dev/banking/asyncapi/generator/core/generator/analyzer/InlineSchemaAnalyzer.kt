package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Promotes inline schemas (objects and enums) within properties, items, and additionalProperties
 * to top-level schemas, replacing them with references.
 */
class InlineSchemaAnalyzer : AnalysisStage<Map<String, Schema>> {

    override fun analyze(schemas: Map<String, Schema>): Map<String, Schema> {
        val schemasWithPromotions = schemas.toMutableMap()
        val namesToProcess = schemasWithPromotions.keys.toMutableList()
        val processedSet = mutableSetOf<String>()

        while (namesToProcess.isNotEmpty()) {
            val schemaName = namesToProcess.removeFirst()
            if (schemaName in processedSet) continue
            processedSet.add(schemaName)

            val originalSchema = schemasWithPromotions[schemaName] ?: continue
            val modifiedSchema = replaceInlineWithRefs(originalSchema, schemaName, schemasWithPromotions, namesToProcess)

            if (modifiedSchema != originalSchema) {
                schemasWithPromotions[schemaName] = modifiedSchema
            }
        }
        return schemasWithPromotions
    }

    private fun replaceInlineWithRefs(
        schema: Schema,
        currentNameHint: String,
        schemas: MutableMap<String, Schema>,
        namesToProcess: MutableList<String>
    ): Schema {
        var modifiedSchema = schema

        // 1. Recurse into properties
        schema.properties?.let { props ->
            val newProps = props.mapValues { (propName, propSchema) ->
                promoteIfNeeded(propSchema, propName, schemas, namesToProcess)
            }
            if (newProps != props) {
                modifiedSchema = modifiedSchema.copy(properties = newProps)
            }
        }

        // 2. Recurse into array items
        schema.items?.let { items ->
            // Use PascalCase of current name + "Item" for array items, or just singularize if needed
            // For now, we use the property name hint itself (which usually becomes plural->singular by usage context)
            val newItems = promoteIfNeeded(items, currentNameHint, schemas, namesToProcess)
            if (newItems != items) {
                modifiedSchema = modifiedSchema.copy(items = newItems)
            }
        }

        // 3. Recurse into additionalProperties (Map values)
        //    If a map has complex inline values (e.g. Map<String, InlineObject>), promote them.
        schema.additionalProperties?.let { additional ->
            val valueNameHint = currentNameHint + "Value"
            val newAdditional = promoteIfNeeded(additional, valueNameHint, schemas, namesToProcess)
            if (newAdditional != additional) {
                modifiedSchema = modifiedSchema.copy(additionalProperties = newAdditional)
            }
        }

        return modifiedSchema
    }

    private fun promoteIfNeeded(
        schemaInterface: SchemaInterface,
        nameHint: String,
        schemas: MutableMap<String, Schema>,
        namesToProcess: MutableList<String>
    ): SchemaInterface {
        if (schemaInterface !is SchemaInterface.SchemaInline) {
            return schemaInterface
        }

        val inlineSchema = schemaInterface.schema
        val processedInlineSchema = replaceInlineWithRefs(inlineSchema, nameHint, schemas, namesToProcess)

        val isInlineObject = processedInlineSchema.type.getPrimaryType() == "object" && !processedInlineSchema.properties.isNullOrEmpty()
        val isInlineEnum = processedInlineSchema.type.getPrimaryType() == "string" && !processedInlineSchema.enum.isNullOrEmpty()

        if (isInlineObject || isInlineEnum) {
            val newSchemaName = processedInlineSchema.title?.takeIf { it.isNotBlank() }
                ?.let { MapperUtil.toPascalCase(it) }
                ?: MapperUtil.toPascalCase(nameHint)

            if (!schemas.containsKey(newSchemaName)) {
                namesToProcess.add(newSchemaName)
            }
            schemas[newSchemaName] = processedInlineSchema

            val newReference = Reference(ref = "#/components/schemas/$newSchemaName", model = processedInlineSchema)
            return SchemaInterface.SchemaReference(reference = newReference)
        }

        // Return the schema even if not promoted (but with its own children potentially promoted)
        return SchemaInterface.SchemaInline(processedInlineSchema)
    }
}
