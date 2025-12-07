package dev.banking.asyncapi.generator.core.generator.java.mapper

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.model.schemas.Schema

class PolymorphicMapper : TypeMapper {
    override fun map(schema: Schema, propertyName: String, root: JavaTypeMapper): String? {
        if (schema.oneOf.isNullOrEmpty() && schema.anyOf.isNullOrEmpty()) return null
        return MapperUtil.toPascalCase(propertyName)
    }
}
