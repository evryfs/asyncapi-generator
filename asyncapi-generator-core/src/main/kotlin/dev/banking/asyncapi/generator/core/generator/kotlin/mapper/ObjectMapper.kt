package dev.banking.asyncapi.generator.core.generator.kotlin.mapper

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class ObjectMapper : TypeMapper {

    override fun map(schema: Schema, propertyName: String, root: KotlinTypeMapper): String? {
        if (schema.type.getPrimaryType() != "object") {
            return null // This mapper only handles objects.
        }

        // If an object has defined 'properties', it implies it should be a generated data class.
        // In this case, ObjectMapper should NOT map it to a generic Map type.
        // If we are here, it means InlineSchemaAnalyzer failed to promote this to a Reference.
        // This is a critical error in the generation pipeline.
        if (!schema.properties.isNullOrEmpty()) {
            // TODO - better error handling
            throw IllegalStateException(
                "ObjectMapper encountered an inline object with properties at property '$propertyName'. " +
                    "This schema should have been promoted to a top-level schema reference by InlineSchemaAnalyzer. " +
                    "This indicates a bug in the generator pipeline."
            )
        }

        // If schema itself has a title but no properties, it means it's a named but empty object.
        // We let it fall through, it will be promoted and become its own generated class.
        schema.title?.takeIf { it.isNotBlank() }?.let { return MapperUtil.toPascalCase(it) }

        // --- LOGIC for additionalProperties ---
        val valueType = when (val additionalProps = schema.additionalProperties) {
            null -> "Any" // Default behavior if additionalProperties is not specified ({})
            is SchemaInterface.BooleanSchema -> if (additionalProps.value) "Any" else "Nothing" // additionalProperties: true/false
            is SchemaInterface.SchemaInline -> {
                // Recursively call the root mapper to find the type of the value schema.
                root.mapKotlinType(propertyName + "Value", additionalProps.schema)
            }
            is SchemaInterface.SchemaReference -> {
                // If the value is a reference, get its type name.
                root.typeNameFromRef(additionalProps.reference)
            }
            else -> "Any" // Fallback for other unexpected SchemaInterface types here.
        }

        // If 'additionalProperties: false', it means no extra properties are allowed.
        // So, this schema doesn't represent an open-ended map.
        if (valueType == "Nothing") {
            return null // Return null, letting it fall back to 'Any' or be an empty data class
        }

        return "Map<String, $valueType>"
    }
}
