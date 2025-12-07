package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.mapper

import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil
import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema

class EnumMapper : TypeMapper {

    override fun map(schema: Schema, propertyName: String, root: KotlinTypeMapper): String? {
        // An enum schema is typically a string type with an 'enum' array.
        // It could also be other types with enum, but string is most common and directly maps to Kotlin enums.
        if (schema.type.getPrimaryType() == "string" && !schema.enum.isNullOrEmpty()) {
            // We generate a specific enum class name, preferring schema title if available,
            // otherwise inferring from the propertyName.
            return schema.title?.takeIf { it.isNotBlank() }
                ?.let { MapperUtil.toPascalCase(it) }
                ?: MapperUtil.toPascalCase(propertyName)
        }
        return null // Not an enum schema.
    }
}
