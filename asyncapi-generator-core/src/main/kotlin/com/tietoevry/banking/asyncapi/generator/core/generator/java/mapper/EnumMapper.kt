package com.tietoevry.banking.asyncapi.generator.core.generator.java.mapper

import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil
import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema

class EnumMapper : TypeMapper {
    override fun map(schema: Schema, propertyName: String, root: JavaTypeMapper): String? {
        if (schema.type.getPrimaryType() == "string" && !schema.enum.isNullOrEmpty()) {
            return schema.title?.takeIf { it.isNotBlank() }
                ?.let { MapperUtil.toPascalCase(it) }
                ?: MapperUtil.toPascalCase(propertyName)
        }
        return null
    }
}
