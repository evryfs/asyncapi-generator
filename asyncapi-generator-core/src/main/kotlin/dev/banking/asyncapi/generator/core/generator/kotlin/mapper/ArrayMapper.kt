package dev.banking.asyncapi.generator.core.generator.kotlin.mapper

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class ArrayMapper(
    val context: GeneratorContext,
) : TypeMapper { // Changed constructor

    override fun map(schema: Schema, propertyName: String, root: KotlinTypeMapper): String? {
        if (schema.type.getPrimaryType() != "array") {
            return null
        }

        val items = schema.items ?: return "List<Any>"
        val elementType = when (items) {
            is SchemaInterface.SchemaInline -> {
                root.mapKotlinType(propertyName, items.schema)
            }

            is SchemaInterface.SchemaReference -> {
                val refName = items.reference.ref.substringAfterLast("/")
                val refSchema = context.findSchemaByName(refName) // Use context for lookup

                val isStringEnum = refSchema?.type.getPrimaryType() == "string" && !refSchema?.enum.isNullOrEmpty()

                if (refSchema?.type.getPrimaryType() == "string" && !isStringEnum) {
                    "String"
                } else {
                    MapperUtil.toPascalCase(refName)
                }
            }

            else -> "Any"
        }
        return "List<$elementType>"
    }
}
