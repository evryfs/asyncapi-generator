package dev.banking.asyncapi.generator.core.generator.normalizer

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Projects conditional property branches into a static schema suitable for
 * generated source models.
 *
 * Branch-only properties remain optional. Conflicting property shapes become
 * open values, and constraints that differ between both branches are omitted
 * rather than applied unconditionally.
 */
class ConditionalNormalizer {
    fun normalize(schemas: Map<String, Schema>): Map<String, Schema> =
        schemas.mapValues { (_, schema) ->
            normalizeSchema(
                schema = schema,
                visiting = Collections.newSetFromMap(IdentityHashMap<Schema, Boolean>()),
            )
        }

    private fun normalizeSchema(
        schema: Schema,
        visiting: MutableSet<Schema>,
    ): Schema {
        if (!visiting.add(schema)) return schema

        try {
            val currentSchema =
                schema.copy(
                    properties =
                        schema.properties?.mapValues { (_, property) ->
                            normalizeNestedSchema(property, visiting) ?: property
                        },
                    items = normalizeNestedSchema(schema.items, visiting),
                    additionalProperties = normalizeNestedSchema(schema.additionalProperties, visiting),
                )
            if (
                currentSchema.ifSchema == null &&
                currentSchema.thenSchema == null &&
                currentSchema.elseSchema == null
            ) {
                return currentSchema
            }

            val thenSchema = resolveBranchSchema(currentSchema.thenSchema, visiting)
            val elseSchema = resolveBranchSchema(currentSchema.elseSchema, visiting)

            return currentSchema.copy(
                properties =
                    mergeProperties(
                        baseProperties = currentSchema.properties.orEmpty(),
                        thenProperties = thenSchema?.properties.orEmpty(),
                        elseProperties = elseSchema?.properties.orEmpty(),
                    ),
                ifSchema = null,
                thenSchema = null,
                elseSchema = null,
            )
        } finally {
            visiting.remove(schema)
        }
    }

    private fun mergeProperties(
        baseProperties: Map<String, SchemaInterface>,
        thenProperties: Map<String, SchemaInterface>,
        elseProperties: Map<String, SchemaInterface>,
    ): Map<String, SchemaInterface> {
        val propertyNames = baseProperties.keys + thenProperties.keys + elseProperties.keys

        return propertyNames.associateWith { propertyName ->
            val baseProperty = baseProperties[propertyName]
            val thenProperty = thenProperties[propertyName]
            val elseProperty = elseProperties[propertyName]
            val properties = listOfNotNull(baseProperty, thenProperty, elseProperty)
            val propertyShapes = properties.map(::propertyShape).toSet()

            when {
                propertyShapes.size > 1 -> openProperty(properties)
                baseProperty != null -> baseProperty
                thenProperty != null && elseProperty != null ->
                    commonBranchProperty(
                        thenProperty = thenProperty,
                        elseProperty = elseProperty,
                    )
                else -> thenProperty ?: elseProperty!!
            }
        }
    }

    private fun commonBranchProperty(
        thenProperty: SchemaInterface,
        elseProperty: SchemaInterface,
    ): SchemaInterface {
        if (
            thenProperty is SchemaInterface.SchemaReference &&
            elseProperty is SchemaInterface.SchemaReference &&
            thenProperty.reference.sourceId == elseProperty.reference.sourceId &&
            thenProperty.reference.ref == elseProperty.reference.ref
        ) {
            return thenProperty
        }

        val thenSchema = thenProperty.resolvedSchema()
        val elseSchema = elseProperty.resolvedSchema()
        return SchemaInterface.SchemaInline(
            Schema(
                type = thenSchema?.type,
                format = thenSchema?.format?.takeIf { format -> format == elseSchema?.format },
                enum = thenSchema?.enum?.takeIf { values -> values == elseSchema?.enum },
                description = thenSchema?.description ?: elseSchema?.description,
            ),
        )
    }

    private fun openProperty(properties: List<SchemaInterface>): SchemaInterface =
        SchemaInterface.SchemaInline(
            Schema(
                description = properties.firstNotNullOfOrNull { property ->
                    property.resolvedSchema()?.description
                },
            ),
        )

    private fun propertyShape(schemaInterface: SchemaInterface): String =
        when (schemaInterface) {
            is SchemaInterface.SchemaInline -> "inline:${schemaInterface.schema.type}"
            is SchemaInterface.SchemaReference ->
                "reference:${schemaInterface.reference.sourceId.orEmpty()}:${schemaInterface.reference.ref}"
            is SchemaInterface.BooleanSchema -> "boolean:${schemaInterface.value}"
            is SchemaInterface.MultiFormatSchemaInline ->
                "multi-format:${schemaInterface.multiFormatSchema.schemaFormat}"
        }

    private fun normalizeNestedSchema(
        schemaInterface: SchemaInterface?,
        visiting: MutableSet<Schema>,
    ): SchemaInterface? =
        if (schemaInterface is SchemaInterface.SchemaInline) {
            SchemaInterface.SchemaInline(
                normalizeSchema(
                    schema = schemaInterface.schema,
                    visiting = visiting,
                ),
            )
        } else {
            schemaInterface
        }

    private fun resolveBranchSchema(
        schemaInterface: SchemaInterface?,
        visiting: MutableSet<Schema>,
    ): Schema? =
        schemaInterface
            ?.resolvedSchema()
            ?.let { schema -> normalizeSchema(schema, visiting) }

    private fun SchemaInterface.resolvedSchema(): Schema? =
        when (this) {
            is SchemaInterface.SchemaInline -> schema
            is SchemaInterface.SchemaReference -> resolveReference(reference) as? Schema
            is SchemaInterface.BooleanSchema,
            is SchemaInterface.MultiFormatSchemaInline,
            -> null
        }

    private fun resolveReference(reference: Reference): Any? {
        val visitedReferences: MutableSet<Reference> =
            Collections.newSetFromMap(IdentityHashMap<Reference, Boolean>())
        var target: Any? = reference
        while (target is Reference && visitedReferences.add(target)) {
            target = target.model
        }
        return target?.takeUnless { it is Reference }
    }
}
