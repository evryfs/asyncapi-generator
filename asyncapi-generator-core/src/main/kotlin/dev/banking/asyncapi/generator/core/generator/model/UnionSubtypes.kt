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
                            ?.let { property ->
                                // An inline enum may have been promoted to a named enum by now.
                                when (property) {
                                    is SchemaInterface.SchemaInline -> property.schema
                                    // Through a reference only a one-value enum identifies the member;
                                    // a shared enum of all values does not.
                                    is SchemaInterface.SchemaReference ->
                                        (property.reference.model as? Schema)?.takeIf { it.const != null || it.enum?.size == 1 }
                                    else -> null
                                }
                            }
                            ?.let { it.const?.toString() ?: it.enum?.firstOrNull()?.toString() }
                            ?.removeSurrounding("\"")
                    }
                UnionSubtype(name = discriminatorValue ?: childSchemaName, type = childSchemaName)
            }
    }
}
