package dev.banking.asyncapi.generator.core.generator.kotlin.mapper

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.hasMultipleNonNullTypes
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class KotlinTypeMapper(
    val context: GeneratorContext,
) {
    fun mapKotlinType(propertyName: String, schema: Schema?): String {
        if (schema == null) return "Any"
        if (schema.type.hasMultipleNonNullTypes()) return "Any"
        if (!schema.oneOf.isNullOrEmpty() || !schema.anyOf.isNullOrEmpty()) {
            return MapperUtil.toPascalCase(propertyName)
        }
        if (schema.type.getPrimaryType() == "string" && !schema.enum.isNullOrEmpty()) {
            return schema.title?.takeIf { it.isNotBlank() }
                ?.let(MapperUtil::toPascalCase)
                ?: MapperUtil.toPascalCase(propertyName)
        }

        return when (schema.type.getPrimaryType()) {
            "string" -> mapString(schema)
            "integer" -> mapInteger(schema)
            "number" -> mapNumber(schema)
            "boolean" -> "Boolean"
            "array" -> mapArray(propertyName, schema)
            "object" -> mapObject(propertyName, schema) ?: "Any"
            else -> "Any"
        }
    }

    fun typeNameFromRef(reference: Reference): String {
        val raw = reference.ref.substringAfterLast("/")
        return MapperUtil.toPascalCase(raw)
    }

    private fun mapString(schema: Schema): String =
        when (schema.format) {
            "uuid" -> "UUID"
            "date-time" -> "OffsetDateTime"
            "date" -> "LocalDate"
            "time" -> "LocalTime"
            else -> "String"
        }

    private fun mapInteger(schema: Schema): String {
        when (schema.format?.lowercase()) {
            "int64" -> return "Long"
            "int32" -> return "Int"
        }
        val minimum = schema.minimum
        val maximum = schema.maximum
        if (minimum != null || maximum != null) {
            val minimumFits = minimum == null || minimum.toDouble() >= Int.MIN_VALUE.toDouble()
            val maximumFits = maximum == null || maximum.toDouble() <= Int.MAX_VALUE.toDouble()
            return if (minimumFits && maximumFits) "Int" else "Long"
        }
        return "Int"
    }

    private fun mapNumber(schema: Schema): String {
        if (schema.multipleOf != null) return "BigDecimal"
        return if (schema.format?.lowercase() == "float") "Float" else "Double"
    }

    private fun mapArray(
        propertyName: String,
        schema: Schema,
    ): String {
        val items = schema.items ?: return "List<Any>"
        val elementType =
            when (items) {
                is SchemaInterface.SchemaInline -> mapKotlinType(propertyName, items.schema)
                is SchemaInterface.SchemaReference -> {
                    val referenceName = items.reference.ref.substringAfterLast("/")
                    val referencedSchema = context.findSchemaByName(referenceName)
                    val isStringEnum =
                        referencedSchema?.type.getPrimaryType() == "string" && !referencedSchema?.enum.isNullOrEmpty()
                    if (referencedSchema?.type.getPrimaryType() == "string" && !isStringEnum) {
                        "String"
                    } else {
                        MapperUtil.toPascalCase(referenceName)
                    }
                }
                else -> "Any"
            }
        return "List<$elementType>"
    }

    private fun mapObject(
        propertyName: String,
        schema: Schema,
    ): String? {
        if (!schema.properties.isNullOrEmpty()) {
            throw IllegalStateException(
                "Kotlin type mapping encountered an inline object with properties at property '$propertyName'. " +
                    "This schema should have been promoted to a top-level schema reference by InlineSchemaAnalyzer. " +
                    "This indicates a bug in the generator pipeline.",
            )
        }
        schema.title?.takeIf { it.isNotBlank() }?.let { return MapperUtil.toPascalCase(it) }
        val valueType =
            when (val additionalProperties = schema.additionalProperties) {
                null -> "Any"
                is SchemaInterface.BooleanSchema -> if (additionalProperties.value) "Any" else return null
                is SchemaInterface.SchemaInline -> mapKotlinType(propertyName + "Value", additionalProperties.schema)
                is SchemaInterface.SchemaReference -> typeNameFromRef(additionalProperties.reference)
                else -> "Any"
            }
        return "Map<String, $valueType>"
    }
}
