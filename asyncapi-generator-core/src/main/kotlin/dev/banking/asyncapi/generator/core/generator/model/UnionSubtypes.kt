package dev.banking.asyncapi.generator.core.generator.model

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * A member of a `oneOf`/`anyOf` union, as Jackson needs it to choose the class to deserialize.
 *
 * @property name Discriminator value that selects this member.
 * @property type Generated type name of the member.
 */
internal data class UnionSubtype(
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
    fun of(schema: Schema, context: GeneratorContext): List<UnionSubtype> {
        val discriminator = schema.discriminator
        return (schema.oneOf ?: schema.anyOf ?: emptyList())
            .filterIsInstance<SchemaInterface.SchemaReference>()
            .map { member ->
                val childSchemaName = member.reference.ref.substringAfterLast('/')
                val discriminatorValue =
                    discriminator?.let { property ->
                        context.findSchemaByName(childSchemaName)
                            ?.properties
                            ?.get(property)
                            ?.let { it as? SchemaInterface.SchemaInline }
                            ?.schema
                            ?.let { it.const?.toString() ?: it.enum?.firstOrNull()?.toString() }
                            ?.removeSurrounding("\"")
                    }
                UnionSubtype(name = discriminatorValue ?: childSchemaName, type = childSchemaName)
            }
    }
}
