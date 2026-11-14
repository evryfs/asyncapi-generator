package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InlineSchemaNameCollision
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Promotes inline object and enum schemas to named schemas, so every generated type has a name.
 *
 * A promoted schema is named after its `title`, or else after the property that holds it.
 * Inline schemas that describe the same type share one name, even when usage-site details such
 * as `default`, `const` or `description` differ; the last one seen is kept, as before. Two schemas
 * of different types that would get the same name fail, because generating both under one name
 * would silently drop one of them.
 * Items of a named array schema are named `<Array>Item`, so they never replace the array.
 */
internal class InlineSchemaAnalyzer {

    fun analyze(schemas: Map<String, Schema>): Map<String, Schema> {
        val promotion = Promotion(schemas)
        while (promotion.namesToProcess.isNotEmpty()) {
            val schemaName = promotion.namesToProcess.removeFirst()
            if (!promotion.processed.add(schemaName)) continue
            val originalSchema = promotion.schemas[schemaName] ?: continue
            val modifiedSchema =
                promotion.replaceInlineWithRefs(
                    schema = originalSchema,
                    nameHint = schemaName,
                    owner = schemaName,
                    location = promotion.locations.getValue(schemaName),
                )
            if (modifiedSchema != originalSchema) {
                promotion.schemas[schemaName] = modifiedSchema
            }
        }
        return promotion.schemas
    }

    private class Promotion(initial: Map<String, Schema>) {
        val schemas = initial.toMutableMap()
        val namesToProcess = initial.keys.toMutableList()
        val processed = mutableSetOf<String>()

        /** Where each named schema comes from, for collision messages. */
        val locations = initial.keys.associateWith { "components.schemas.$it" }.toMutableMap()

        fun replaceInlineWithRefs(
            schema: Schema,
            nameHint: String,
            owner: String,
            location: String,
        ): Schema {
            var modifiedSchema = schema
            schema.properties?.let { props ->
                val newProps = props.mapValues { (propName, propSchema) ->
                    promoteIfNeeded(propSchema, propName, owner, "$location.properties.$propName")
                }
                if (newProps != props) {
                    modifiedSchema = modifiedSchema.copy(properties = newProps)
                }
            }
            schema.items?.let { items ->
                val itemsHint = if (MapperUtil.toPascalCase(nameHint) == owner) "${owner}Item" else nameHint
                val newItems = promoteIfNeeded(items, itemsHint, owner, "$location.items")
                if (newItems != items) {
                    modifiedSchema = modifiedSchema.copy(items = newItems)
                }
            }
            schema.additionalProperties?.let { additional ->
                val newAdditional =
                    promoteIfNeeded(additional, nameHint + "Value", owner, "$location.additionalProperties")
                if (newAdditional != additional) {
                    modifiedSchema = modifiedSchema.copy(additionalProperties = newAdditional)
                }
            }
            return modifiedSchema
        }

        private fun promoteIfNeeded(
            schemaInterface: SchemaInterface,
            nameHint: String,
            owner: String,
            location: String,
        ): SchemaInterface {
            if (schemaInterface !is SchemaInterface.SchemaInline) {
                return schemaInterface
            }

            val processedInlineSchema = replaceInlineWithRefs(schemaInterface.schema, nameHint, owner, location)
            val primaryType = processedInlineSchema.type.getPrimaryType()
            val isInlineObject = primaryType == "object" && !processedInlineSchema.properties.isNullOrEmpty()
            val isInlineEnum = primaryType == "string" && !processedInlineSchema.enum.isNullOrEmpty()
            if (!isInlineObject && !isInlineEnum) {
                return SchemaInterface.SchemaInline(processedInlineSchema)
            }

            val newSchemaName =
                processedInlineSchema.title?.takeIf { it.isNotBlank() }?.let { MapperUtil.toPascalCase(it) }
                    ?: MapperUtil.toPascalCase(nameHint)
            val existing = schemas[newSchemaName]
            when {
                existing == null -> {
                    schemas[newSchemaName] = processedInlineSchema
                    locations[newSchemaName] = location
                    namesToProcess.add(newSchemaName)
                }
                typeShape(existing) == typeShape(processedInlineSchema) ->
                    schemas[newSchemaName] = processedInlineSchema
                else -> throw InlineSchemaNameCollision(
                    schemaName = newSchemaName,
                    location = location,
                    existingLocation = locations[newSchemaName],
                )
            }
            val newReference = Reference(ref = "#/components/schemas/$newSchemaName", model = processedInlineSchema)
            return SchemaInterface.SchemaReference(reference = newReference)
        }

        /** What the generated type depends on: its kind, values, and the shapes of its members. */
        private fun typeShape(schema: Schema): List<Any?> =
            listOf(
                schema.type.getPrimaryType(),
                schema.format,
                schema.enum,
                schema.required?.toSet(),
                schema.properties?.mapValues { (_, property) -> typeShape(property) },
                schema.items?.let(::typeShape),
                schema.additionalProperties?.let(::typeShape),
            )

        private fun typeShape(schema: SchemaInterface): Any? =
            when (schema) {
                is SchemaInterface.SchemaInline -> typeShape(schema.schema)
                is SchemaInterface.SchemaReference -> schema.reference.ref
                else -> schema
            }
    }
}
