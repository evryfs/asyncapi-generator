package com.tietoevry.banking.asyncapi.generator.core.generator.java.mapper

import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema

class NumericMapper : TypeMapper {

    override fun map(schema: Schema, propertyName: String, root: JavaTypeMapper): String? {
        return when (schema.type.getPrimaryType()) {
            "integer" -> mapInteger(schema)
            "number" -> mapNumber(schema)
            else -> null
        }
    }

    private fun mapInteger(schema: Schema): String {
        val format = schema.format?.lowercase()
        when (format) {
            "int64" -> return "Long"
            "int32" -> return "Integer"
        }
        // Boundary check
        val minimum = schema.minimum
        val maximum = schema.maximum
        if (minimum != null || maximum != null) {
            val minOk = minimum == null || minimum.toDouble() >= Int.MIN_VALUE.toDouble()
            val maxOk = maximum == null || maximum.toDouble() <= Int.MAX_VALUE.toDouble()
            if (minOk && maxOk) {
                return "Integer"
            }
            return "Long"
        }
        return "Integer"
    }

    private fun mapNumber(schema: Schema): String {
        if (schema.multipleOf != null) {
            return "BigDecimal"
        }
        return when (schema.format?.lowercase()) {
            "float" -> "Float"
            "double" -> "Double"
            else -> "Double"
        }
    }
}
