package dev.banking.asyncapi.generator.core.generator.avro.mapper

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Maps JSON Schema types to Avro type JSON.
 *
 * Named schemas are referenced by their full name, `<namespace>.<SchemaName>`, which is the
 * name their own `.avsc` file declares. Arrays and maps map their items and values recursively.
 */
class AvroTypeMapper(
    val packageName: String,
) {
    fun mapToAvroType(schema: Schema?, isOptional: Boolean, refName: String? = null): String {
        val type = requiredType(schema, refName)
        if (!isOptional) return type
        // Avro unions cannot nest, so an optional union gets "null" as its first branch.
        return if (type.startsWith("[")) "[\"null\", ${type.removePrefix("[")}" else "[\"null\", $type]"
    }

    /** The Avro type of a field that must hold a value; [refName] names a referenced schema. */
    fun requiredType(schema: Schema?, refName: String? = null): String {
        if (schema != null && !schema.oneOf.isNullOrEmpty()) {
            val unionTypes = schema.oneOf.filterIsInstance<SchemaInterface.SchemaReference>().map(::namedType)
            if (unionTypes.isNotEmpty()) return unionTypes.joinToString(", ", "[", "]")
        }
        if (refName != null) return namedType(refName)
        if (schema == null) return "\"string\""

        return when (schema.type.getPrimaryType()) {
            "array" -> "{\"type\": \"array\", \"items\": ${elementType(schema.items)}}"
            "object" -> "{\"type\": \"map\", \"values\": ${elementType(schema.additionalProperties)}}"
            else -> baseType(schema)
        }
    }

    private fun elementType(element: SchemaInterface?): String =
        when (element) {
            is SchemaInterface.SchemaReference -> namedType(element)
            is SchemaInterface.SchemaInline -> requiredType(element.schema)
            else -> "\"string\""
        }

    private fun namedType(reference: SchemaInterface.SchemaReference): String =
        namedType(reference.reference.ref.substringAfterLast('/'))

    /** Schema names reach the generator in PascalCase, which is also the record name in its own file. */
    private fun namedType(schemaName: String): String = "\"$packageName.${MapperUtil.toPascalCase(schemaName)}\""

    private fun baseType(schema: Schema): String =
        when (schema.type.getPrimaryType()) {
            "string" ->
                when (schema.format) {
                    "uuid" -> "{\"type\": \"string\", \"logicalType\": \"uuid\"}"
                    "date" -> "{\"type\": \"int\", \"logicalType\": \"date\"}"
                    "date-time" -> "{\"type\": \"long\", \"logicalType\": \"timestamp-millis\"}"
                    else -> "\"string\""
                }
            "integer" ->
                when (schema.format) {
                    "date" -> "{\"type\": \"int\", \"logicalType\": \"date\"}"
                    "int64" -> "\"long\""
                    else -> "\"int\""
                }
            // Always double: narrowing an existing double field to float breaks registered schemas.
            "number" -> "\"double\""
            "boolean" -> "\"boolean\""
            else -> "\"string\""
        }
}
