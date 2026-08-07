package dev.banking.asyncapi.generator.core.generator.normalizer

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Flattens `allOf` compositions into the static schema shape consumed by the
 * source and projection generators.
 *
 * Resolved references remain authoritative. Inline object properties, array
 * items, and map values are traversed recursively, with schema identity used
 * to terminate recursive compositions.
 */
class CompositionNormalizer {
    private val schemaMerger = SchemaMerger()

    fun normalize(schemas: Map<String, Schema>): Map<String, Schema> =
        schemas.mapValues { (name, schema) ->
            resolveSchemaRecursive(
                schema = schema,
                schemaName = name,
                visiting = Collections.newSetFromMap(IdentityHashMap<Schema, Boolean>()),
            )
        }

    private fun resolveSchemaRecursive(
        schema: Schema,
        schemaName: String,
        visiting: MutableSet<Schema>,
    ): Schema {
        if (!visiting.add(schema)) return schema

        try {
            val currentSchema =
                schema.copy(
                    properties =
                        schema.properties?.mapValues { (propertyName, propertySchema) ->
                            normalizeNestedSchema(
                                schemaInterface = propertySchema,
                                schemaName = "$schemaName.$propertyName",
                                visiting = visiting,
                            ) ?: propertySchema
                        },
                    items =
                        normalizeNestedSchema(
                            schemaInterface = schema.items,
                            schemaName = "$schemaName.items",
                            visiting = visiting,
                        ),
                    additionalProperties =
                        normalizeNestedSchema(
                            schemaInterface = schema.additionalProperties,
                            schemaName = "$schemaName.additionalProperties",
                            visiting = visiting,
                        ),
                )

            if (currentSchema.allOf.isNullOrEmpty()) {
                return currentSchema
            }

            var mergedSchema = currentSchema
            var titleFromReference: String? = null

            currentSchema.allOf.forEach { schemaInterface ->
                if (schemaInterface is SchemaInterface.SchemaReference) {
                    titleFromReference = schemaInterface.reference.ref.substringAfterLast('/')
                }

                resolveSubSchema(
                    schemaInterface = schemaInterface,
                    parentName = schemaName,
                    visiting = visiting,
                )?.let { parentSchema ->
                    mergedSchema = schemaMerger.merge(parentSchema, mergedSchema)
                }
            }

            return mergedSchema.copy(
                allOf = null,
                title = mergedSchema.title ?: titleFromReference,
            )
        } finally {
            visiting.remove(schema)
        }
    }

    private fun resolveSubSchema(
        schemaInterface: SchemaInterface,
        parentName: String,
        visiting: MutableSet<Schema>,
    ): Schema? =
        when (schemaInterface) {
            is SchemaInterface.SchemaInline ->
                resolveSchemaRecursive(
                    schema = schemaInterface.schema,
                    schemaName = "$parentName.allOf",
                    visiting = visiting,
                )
            is SchemaInterface.SchemaReference -> {
                val referencedSchema =
                    resolveReference(schemaInterface.reference)
                    ?: throw IllegalArgumentException(
                        "Schema normalization requires allOf reference " +
                            "'${schemaInterface.reference.ref}' in schema '$parentName' to resolve to a Schema.",
                    )
                resolveSchemaRecursive(
                    schema = referencedSchema,
                    schemaName = schemaInterface.reference.ref,
                    visiting = visiting,
                )
            }
            else -> null
        }

    private fun normalizeNestedSchema(
        schemaInterface: SchemaInterface?,
        schemaName: String,
        visiting: MutableSet<Schema>,
    ): SchemaInterface? =
        if (schemaInterface is SchemaInterface.SchemaInline) {
            SchemaInterface.SchemaInline(
                resolveSchemaRecursive(
                    schema = schemaInterface.schema,
                    schemaName = schemaName,
                    visiting = visiting,
                ),
            )
        } else {
            schemaInterface
        }

    private fun resolveReference(reference: Reference): Schema? {
        val visitedReferences: MutableSet<Reference> =
            Collections.newSetFromMap(IdentityHashMap<Reference, Boolean>())
        var target: Any? = reference
        while (target is Reference && visitedReferences.add(target)) {
            target = target.model
        }
        return target as? Schema
    }
}
