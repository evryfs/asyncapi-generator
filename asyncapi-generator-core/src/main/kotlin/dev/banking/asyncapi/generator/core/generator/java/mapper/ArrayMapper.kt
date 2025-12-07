package dev.banking.asyncapi.generator.core.generator.java.mapper

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class ArrayMapper(val context: GeneratorContext) : TypeMapper {

    override fun map(schema: Schema, propertyName: String, root: JavaTypeMapper): String? {
        if (schema.type.getPrimaryType() != "array") return null

        val items = schema.items ?: return "List<Object>"
        val elementType = when (items) {
            is SchemaInterface.SchemaInline -> root.mapJavaType(propertyName, items.schema)
            is SchemaInterface.SchemaReference -> {
                val refName = items.reference.ref.substringAfterLast("/")
                val refSchema = context.findSchemaByName(refName)
                // Check if enum
                val isStringEnum = refSchema?.type.getPrimaryType() == "string" && !refSchema?.enum.isNullOrEmpty()
                if (refSchema?.type.getPrimaryType() == "string" && !isStringEnum) {
                    "String"
                } else {
                    MapperUtil.toPascalCase(refName)
                }
            }
            else -> "Object"
        }
        return "List<$elementType>"
    }
}
