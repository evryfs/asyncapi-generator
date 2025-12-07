package com.tietoevry.banking.asyncapi.generator.core.generator.java.mapper

import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema

class StringMapper : TypeMapper {
    override fun map(schema: Schema, propertyName: String, root: JavaTypeMapper): String? {
        if (schema.type.getPrimaryType() != "string") {
            return null
        }
        return when (schema.format) {
            "uuid" -> "UUID"
            "date-time" -> "OffsetDateTime"
            "date" -> "LocalDate"
            "time" -> "LocalTime"
            "email" -> "String"
            else -> "String"
        }
    }
}
