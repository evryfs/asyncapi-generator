package dev.banking.asyncapi.generator.core.generator.kotlin.mapper

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class ObjectMapper : TypeMapper {

    override fun map(schema: Schema, propertyName: String, root: KotlinTypeMapper): String? {
        if (schema.type.getPrimaryType() != "object") {
            return null
        }
        if (!schema.properties.isNullOrEmpty()) {
            throw IllegalStateException(
                "ObjectMapper encountered an inline object with properties at property '$propertyName'. " +
                    "This schema should have been promoted to a top-level schema reference by InlineSchemaAnalyzer. " +
                    "This indicates a bug in the generator pipeline."
            )
        }
        schema.title?.takeIf { it.isNotBlank() }?.let { return MapperUtil.toPascalCase(it) }
        val valueType = when (val additionalProps = schema.additionalProperties) {
            null -> "Any" // Default behavior if additionalProperties is not specified ({})
            is SchemaInterface.BooleanSchema -> if (additionalProps.value) "Any" else "Nothing" // additionalProperties: true/false
            is SchemaInterface.SchemaInline -> {
                root.mapKotlinType(propertyName + "Value", additionalProps.schema)
            }
            is SchemaInterface.SchemaReference -> {
                root.typeNameFromRef(additionalProps.reference)
            }
            else -> "Any" // Fallback for other unexpected SchemaInterface types here.
        }

        if (valueType == "Nothing") {
            return null // Return null, letting it fall back to 'Any' or be an empty data class
        }

        return "Map<String, $valueType>"
    }
}
