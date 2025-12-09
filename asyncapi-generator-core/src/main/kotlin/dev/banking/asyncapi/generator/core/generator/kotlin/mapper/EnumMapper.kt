package dev.banking.asyncapi.generator.core.generator.kotlin.mapper

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema

class EnumMapper : TypeMapper {

    override fun map(schema: Schema, propertyName: String, root: KotlinTypeMapper): String? {
        if (schema.type.getPrimaryType() == "string" && !schema.enum.isNullOrEmpty()) {
            return schema.title?.takeIf { it.isNotBlank() }
                ?.let { MapperUtil.toPascalCase(it) }
                ?: MapperUtil.toPascalCase(propertyName)
        }
        return null
    }
}
