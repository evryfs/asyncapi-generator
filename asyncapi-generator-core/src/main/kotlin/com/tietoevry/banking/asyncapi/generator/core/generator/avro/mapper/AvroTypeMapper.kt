package com.tietoevry.banking.asyncapi.generator.core.generator.avro.mapper

import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil
import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class AvroTypeMapper(
    val packageName: String,
    ) {

    fun mapToAvroType(schema: Schema?, isOptional: Boolean, refName: String? = null): String {
        // 1. Handle Polymorphism (Inline Union)
        // We check the schema object itself. If it defines 'oneOf', we MUST generate an Avro Union
        // instead of referencing the schema by name (because the named schema 'MySchema' doesn't exist as a record).
        if (schema != null && !schema.oneOf.isNullOrEmpty()) {
            val unionTypes = schema.oneOf.mapNotNull { ref ->
                if (ref is SchemaInterface.SchemaReference) {
                    val childName = ref.reference.ref.substringAfterLast('/')
                    "\"$packageName.${MapperUtil.toPascalCase(childName)}\""
                } else null
            }

            if (unionTypes.isNotEmpty()) {
                return if (isOptional) {
                    // Merge "null" into the union list: ["null", "Com.A", "Com.B"]
                    val combined = mutableListOf("\"null\"")
                    combined.addAll(unionTypes)
                    combined.joinToString(", ", "[", "]")
                } else {
                    // Standard union: ["Com.A", "Com.B"]
                    unionTypes.joinToString(", ", "[", "]")
                }
            }
        }

        // 2. Handle Named References (Standard Records)
        if (refName != null) {
            val pascalName = MapperUtil.toPascalCase(refName)
            val fullName = "\"$packageName.$pascalName\""
            return if (isOptional) "[\"null\", $fullName]" else fullName
        }

        // 3. Handle Primitives (Inline schemas or unknown)
        if (schema == null) return "\"string\""

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
                // Simplified array logic for primitives
                "{\"type\": \"array\", \"items\": \"string\"}"
            }
            else -> "\"string\""
        }
    }
}
