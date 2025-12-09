package dev.banking.asyncapi.generator.core.generator.avro.mapper

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class AvroTypeMapper(
    val packageName: String,
) {

    fun mapToAvroType(schema: Schema?, isOptional: Boolean, refName: String? = null): String {
        if (schema != null && !schema.oneOf.isNullOrEmpty()) {
            val unionTypes = schema.oneOf.mapNotNull { ref ->
                if (ref is SchemaInterface.SchemaReference) {
                    val childName = ref.reference.ref.substringAfterLast('/')
                    "\"$packageName.${MapperUtil.toPascalCase(childName)}\""
                } else null
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

        if (schema == null) {
            return "\"string\""
        }
        val baseType = resolveBaseType(schema)
        val finalType = if (schema.type.getPrimaryType() == "integer" && schema.format == "int64") {
            "\"long\""
        } else {
            baseType
        }
        return if (isOptional) {
            "[\"null\", $finalType]"
        } else {
            finalType
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
            "array" -> {
                "{\"type\": \"array\", \"items\": \"string\"}"
            }
            else -> "\"string\""
        }
    }
}
