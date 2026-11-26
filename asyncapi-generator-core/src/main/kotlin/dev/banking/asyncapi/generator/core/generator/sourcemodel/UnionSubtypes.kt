package dev.banking.asyncapi.generator.core.generator.sourcemodel

import dev.banking.asyncapi.generator.core.generator.schema.generatedSchemaName
import dev.banking.asyncapi.generator.core.generator.schema.linkedSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * A member of a `oneOf`/`anyOf` union, as Jackson needs it to choose the class to deserialize.
 *
 * @property name Discriminator value that selects this member.
 * @property type Generated type name of the member.
 */
data class UnionSubtype(
    val name: String,
    val type: String,
)

/**
 * Finds the members of a union schema and the discriminator value of each.
 *
 * A member's value is the `const` or first `enum` value of its discriminator property, and
 * otherwise its schema name, as the AsyncAPI discriminator defines. Inline members have no
 * generated type and are skipped.
 */
internal object UnionSubtypes {
    fun of(
        schema: Schema,
        schemas: Map<String, Schema>,
    ): List<UnionSubtype> {
        val discriminator = schema.discriminator
        return (schema.oneOf ?: schema.anyOf ?: emptyList())
            .filterIsInstance<SchemaInterface.SchemaReference>()
            .map { member ->
                val childSchemaName = member.reference.generatedSchemaName()
                val discriminatorValue =
                    discriminator
                        ?.let { schemas[childSchemaName]?.properties?.get(it) }
                        ?.let { discriminatorSchema(it, schemas) }
                        ?.let { it.const?.toString() ?: it.enum?.firstOrNull()?.toString() }
                UnionSubtype(
                    name = discriminatorValue ?: member.reference.ref.substringAfterLast('/'),
                    type = childSchemaName,
                )
            }
    }

    /**
     * The schema that fixes a member's discriminator value. An inline enum may have been
     * promoted to a named enum by now. Through a reference only a one-value enum or a const
     * identifies the member, as a shared enum of all values does not; the normalized schema
     * counts, so a const added through `allOf` identifies it too.
     */
    private fun discriminatorSchema(
        property: SchemaInterface,
        schemas: Map<String, Schema>,
    ): Schema? =
        when (property) {
            is SchemaInterface.SchemaInline -> property.schema
            is SchemaInterface.SchemaReference ->
                (schemas[property.reference.generatedSchemaName()] ?: property.linkedSchema())
                    ?.takeIf { it.const != null || it.enum?.size == 1 }
            else -> null
        }
}
