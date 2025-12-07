package com.tietoevry.banking.asyncapi.generator.core.generator.java.mapper

import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil
import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class ObjectMapper : TypeMapper {

    override fun map(schema: Schema, propertyName: String, root: JavaTypeMapper): String? {
        if (schema.type.getPrimaryType() != "object") return null
        if (!schema.properties.isNullOrEmpty()) {
            throw IllegalStateException(
                "ObjectMapper (Java) encountered an inline object with properties at property '$propertyName'. " +
                    "This indicates a bug in the generator pipeline."
            )
        }
        schema.title?.takeIf { it.isNotBlank() }?.let { return MapperUtil.toPascalCase(it) }
        val valueType = when (val additionalProps = schema.additionalProperties) {
            null -> "Object"
            is SchemaInterface.BooleanSchema -> if (additionalProps.value) "Object" else "Void"
            is SchemaInterface.SchemaInline -> root.mapJavaType(propertyName + "Value", additionalProps.schema)
            is SchemaInterface.SchemaReference -> root.typeNameFromRef(additionalProps.reference)
            else -> "Object"
        }
        if (valueType == "Void") return null
        return "Map<String, $valueType>"
    }
}
