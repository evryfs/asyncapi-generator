package dev.banking.asyncapi.generator.core.generator.kotlin.mapper

import dev.banking.asyncapi.generator.core.model.schemas.Schema

/**
 * Defines the contract for a single-responsibility mapper that attempts to map a
 * JSON Schema definition to a Kotlin type. If a mapper cannot handle the given
 * schema, it should return null, allowing the next mapper in the chain to try.
 */
interface TypeMapper {

    /**
     * Attempts to map the given schema to a Kotlin type name.
     *
     * @param schema The schema object to map.
     * @param propertyName The name of the property this schema defines, used for inferring names.
     * @param root A reference to the root mapper, used for recursive mapping calls.
     * @return A string containing the Kotlin type name if the schema is handled, otherwise null.
     */
    fun map(schema: Schema, propertyName: String, root: KotlinTypeMapper): String?
}
