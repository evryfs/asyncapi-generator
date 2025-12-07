package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.mapper

import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema

class BooleanMapper : TypeMapper {

    override fun map(schema: Schema, propertyName: String, root: KotlinTypeMapper): String? {
        if (schema.type.getPrimaryType() != "boolean") {
            return null // This mapper only handles boolean
        }
        return "Boolean"
    }
}
