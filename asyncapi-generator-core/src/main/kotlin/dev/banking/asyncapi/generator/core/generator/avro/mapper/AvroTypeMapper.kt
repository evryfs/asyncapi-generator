package dev.banking.asyncapi.generator.core.generator.avro.mapper

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class AvroTypeMapper(
    val packageName: String,
) {
    fun getTitleFromReference(ref: SchemaInterface.SchemaReference): String {
        val childName = ref.reference.ref.substringAfterLast('/')
        return "$packageName.${MapperUtil.toPascalCase(childName)}"
    }

    fun mapToAvroType(schema: Schema?, isOptional: Boolean, refName: String? = null): String {
        if (schema != null && !schema.oneOf.isNullOrEmpty()) {
            val unionTypes = schema.oneOf.mapNotNull { ref ->
                if (ref is SchemaInterface.SchemaReference)
                    "\"${getTitleFromReference(ref)}\""
                else null
            }

            if (unionTypes.isNotEmpty()) {
                return if (isOptional) {
                    val combined = mutableListOf("\"null\"")
                    combined.addAll(unionTypes)
                    combined.joinToString(", ", "[", "]")
                } else {
                    unionTypes.joinToString(", ", "[", "]")
                }
            }
        }

        if (refName != null) {
            val pascalName = MapperUtil.toPascalCase(refName)
            val fullName = "\"$packageName.$pascalName\""
            return if (isOptional) "[\"null\", $fullName]" else fullName
        }

        if (schema == null)
            return "\"string\""

        val finalType = if (schema.type.getPrimaryType() == "integer" && schema.format == "int64")
            "\"long\""
        else if (schema.type.getPrimaryType() == "array")
            "{\"type\": \"array\", \"items\": ${resolveArrayItemsType(schema)}}"
        else if (schema.type.getPrimaryType() == "object")
            "{\"type\": \"map\", \"values\": ${resolveMapValuesType(schema)}}"
        else
            resolveBaseType(schema)
        return if (isOptional) {
            "[\"null\", $finalType]"
        } else {
            finalType
        }
    }

    private fun resolveArrayItemsType(schema: Schema): String {
        try {
            val defaultSchema = "\"string\""
            if (schema.items == null)
                return defaultSchema
            if (schema.items is SchemaInterface.SchemaInline && schema.items.schema.type.getPrimaryType() != null)
                return resolveBaseType(schema.items.schema)
                    ?: defaultSchema
            else if (schema.items is SchemaInterface.SchemaReference)
                return "\"${getTitleFromReference(schema.items)}\""
            return defaultSchema
        } catch (e: Exception) {
            throw RuntimeException("Error while resolving map values $schema")
        }
    }

    private fun resolveMapValuesType(schema: Schema): String {
        try {
            val defaultSchema = "\"string\""
            if (schema.additionalProperties == null)
                return defaultSchema
            else if (schema.additionalProperties is SchemaInterface.SchemaInline)
                return resolveBaseType(schema.additionalProperties.schema)
                    ?: defaultSchema
            else if (schema.additionalProperties is SchemaInterface.SchemaReference)
                return "\"${getTitleFromReference(schema.additionalProperties)}\""
            return defaultSchema
        } catch (e: Exception) {
            throw RuntimeException("Error while resolving map values $schema")
        }
    }

    private fun resolveBaseType(schema: Schema): String {
        return when (schema.type.getPrimaryType()) {
            "string" -> {
                when (schema.format) {
                    "uuid" -> "{\"type\": \"string\", \"logicalType\": \"uuid\"}"
                    "date" -> "{\"type\": \"int\", \"logicalType\": \"date\"}"
                    "date-time" -> "{\"type\": \"long\", \"logicalType\": \"timestamp-millis\"}"
                    else -> "\"string\""
                }
            }

            "integer" -> {
                if (schema.format == "date") "{\"type\": \"int\", \"logicalType\": \"date\"}"
                else "\"int\""
            }

            "number" -> "\"double\""
            "boolean" -> "\"boolean\""
            "long" -> {
                if (schema.format == "date-time") "{\"type\": \"long\", \"logicalType\": \"timestamp-millis\"}"
                else "\"long\""
            }

            else -> "\"string\""
        }
    }
}