package com.tietoevry.banking.asyncapi.generator.core.generator.java.mapper

import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema

class BooleanMapper : TypeMapper {
    override fun map(schema: Schema, propertyName: String, root: JavaTypeMapper): String? {
        if (schema.type.getPrimaryType() != "boolean") return null
        return "Boolean"
    }
}
