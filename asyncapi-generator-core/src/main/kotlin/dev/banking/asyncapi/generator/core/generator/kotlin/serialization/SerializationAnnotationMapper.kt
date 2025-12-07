package dev.banking.asyncapi.generator.core.generator.kotlin.serialization

import dev.banking.asyncapi.generator.core.model.schemas.Schema

/**
 * Maps schema's readOnly and writeOnly flags to Jackson's @JsonProperty annotations.
 * This class isolates serialization-specific annotation logic.
 */
class SerializationAnnotationMapper(
    val framework: String,
    // If we choose to provide explicit property names in annotations
    val includeJsonPropertyName: Boolean = false
) {



    /**
     * Builds serialization annotations based on schema flags and the configured framework.
     * @param propertyName The original JSON property name.
     * @param schema The schema for the property.
     * @return A list of annotation strings.
     */
    // Modified to always accept propertyName, but its use is conditional
    fun buildAnnotations(propertyName: String, schema: Schema?): List<String> {
        if (schema == null) return emptyList()

        return when (framework) {
            "jackson" -> buildJacksonAnnotations(propertyName, schema)
            "avro" -> emptyList() // Avro schemas are external. No direct annotations on data classes for this.
            "kotlinx" -> emptyList() // kotlinx.serialization uses different mechanisms (e.g., @Transient, custom serializers).
            else -> emptyList() // Unknown framework, no annotations
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
            // Only add if explicit name inclusion is requested, even if no access modifier
            annotations += "@JsonProperty(\"$propertyName\")"
        }

        return annotations
    }
}
