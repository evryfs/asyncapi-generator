package com.tietoevry.banking.asyncapi.generator.core.generator.java.serialization

import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema

/**
 * Maps schema's readOnly and writeOnly flags to Jackson's @JsonProperty annotations.
 */
class SerializationAnnotationMapper(
    val framework: String,
    val includeJsonPropertyName: Boolean = false
) {

    fun buildAnnotations(propertyName: String, schema: Schema?): List<String> {
        if (schema == null) return emptyList()

        return when (framework) {
            "jackson" -> buildJacksonAnnotations(propertyName, schema)
            "avro" -> emptyList()
            else -> emptyList()
        }
    }

    private fun buildJacksonAnnotations(propertyName: String, schema: Schema): List<String> {
        val annotations = mutableListOf<String>()
        val isReadOnly = schema.readOnly == true
        val isWriteOnly = schema.writeOnly == true

        val accessPart = when {
            isReadOnly && isWriteOnly -> "access = Access.READ_WRITE"
            isReadOnly -> "access = Access.READ_ONLY"
            isWriteOnly -> "access = Access.WRITE_ONLY"
            else -> null
        }

        val namePart = if (includeJsonPropertyName) "value = \"$propertyName\"" else null

        val args = mutableListOf<String>()
        namePart?.let { args.add(it) }
        accessPart?.let { args.add(it) }

        if (args.isNotEmpty()) {
            annotations += "@JsonProperty(${args.joinToString(", ")})"
        } else if (includeJsonPropertyName) {
            annotations += "@JsonProperty(\"$propertyName\")"
        }

        return annotations
    }
}
