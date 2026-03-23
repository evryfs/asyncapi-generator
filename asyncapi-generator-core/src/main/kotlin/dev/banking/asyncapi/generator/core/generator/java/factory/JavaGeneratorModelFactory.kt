package dev.banking.asyncapi.generator.core.generator.java.factory

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidJavaEnumLiteral
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.JavaEnumLiteralCollision
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import kotlin.text.trimStart

class JavaGeneratorModelFactory(
    val packageName: String,
    val context: GeneratorContext,
    val polymorphicRelationships: Map<String, List<String>>,
) {
    private val propertyFactory = PropertyFactory(context)
    private val javaEnumIdentifierRegex = Regex("^[A-Z_][A-Z0-9_]*$")

    fun create(
        name: String,
        schema: Schema,
    ): GeneratorItem? {
        val isUnionType = !schema.oneOf.isNullOrEmpty() || !schema.anyOf.isNullOrEmpty()
        val isEnum = schema.type.getPrimaryType() == "string" && !schema.enum.isNullOrEmpty()
        val isObject = schema.type.getPrimaryType() == "object"
        val isOpenPayload = isOpenPayloadSchema(schema)

        val description = DocumentationUtils.toJavaDocLines(schema.description)

        return when {
            isEnum ->
                GeneratorItem.EnumModel(
                    name = name,
                    packageName = packageName,
                    description = description,
                    values = validateAndNormalizeEnumValues(name, schema.enum),
                )
            isUnionType -> {
                val discriminatorPropertyName = schema.discriminator

                val subTypes =
                    (schema.oneOf ?: schema.anyOf ?: emptyList())
                        .mapNotNull { ref ->
                            if (ref is SchemaInterface.SchemaReference) {
                                val childSchemaName = ref.reference.ref.substringAfterLast('/')
                                val childSchema = context.findSchemaByName(childSchemaName)

                                // Attempt to find the discriminator value in the child schema
                                val discriminatorValue =
                                    childSchema
                                        ?.properties
                                        ?.get(discriminatorPropertyName)
                                        ?.let {
                                            when (it) {
                                                is SchemaInterface.SchemaInline ->
                                                    it.schema.const?.toString()
                                                        ?: it.schema.enum
                                                            ?.firstOrNull()
                                                            ?.toString()
                                                else -> null
                                            }
                                        }?.removeSurrounding("\"")

                                if (discriminatorValue != null) {
                                    GeneratorItem.InterfaceModel.SubType(name = discriminatorValue, type = childSchemaName)
                                } else {
                                    // Fallback if discriminator value not found, use schema name
                                    GeneratorItem.InterfaceModel.SubType(name = childSchemaName, type = childSchemaName)
                                }
                            } else {
                                null // Non-reference schemas for oneOf are not directly supported as subtypes here
                            }
                        }

                GeneratorItem.InterfaceModel(
                    name = name,
                    packageName = packageName,
                    description = description,
                    discriminator = discriminatorPropertyName,
                    subTypes = subTypes,
                )
            }

            isOpenPayload -> null
            isObject -> {
                val properties =
                    schema.properties?.map { (propName, propSchema) ->
                        propertyFactory.createProperty(propName, propSchema, schema.required ?: emptyList())
                    } ?: emptyList()
                val interfaces = (polymorphicRelationships[name] ?: emptyList()) + "Serializable"
                GeneratorItem.ClassModel(
                    name = name,
                    packageName = packageName,
                    description = description,
                    properties = properties,
                    implementsInterfaces = interfaces,
                )
            }

            else -> null
        }
    }

    private fun isOpenPayloadSchema(schema: Schema): Boolean {
        if (schema.type == null) {
            return schema.properties.isNullOrEmpty() &&
                schema.additionalProperties == null &&
                schema.enum.isNullOrEmpty() &&
                schema.oneOf.isNullOrEmpty() &&
                schema.anyOf.isNullOrEmpty() &&
                schema.allOf.isNullOrEmpty()
        }
        if (schema.type.getPrimaryType() != "object") return false
        if (!schema.properties.isNullOrEmpty()) return false
        return when (val additional = schema.additionalProperties) {
            null -> true
            is SchemaInterface.BooleanSchema -> additional.value
            is SchemaInterface.SchemaInline ->
                additional.schema.type == null &&
                    additional.schema.properties.isNullOrEmpty() &&
                    additional.schema.additionalProperties == null

            else -> false
        }
    }

    private fun validateAndNormalizeEnumValues(
        schemaName: String,
        rawValues: List<Any?>,
    ): List<String> {
        val normalizedToOriginals = linkedMapOf<String, MutableList<String>>()
        val normalizedValues =
            rawValues.map { raw ->
                val original = raw.toEnumLiteral()
                val normalized = normalizeEnumLiteral(original)
                if (!javaEnumIdentifierRegex.matches(normalized)) {
                    throw InvalidJavaEnumLiteral(
                        schemaName = schemaName,
                        literal = original,
                        normalized = normalized,
                        packageName = packageName,
                    )
                }
                normalizedToOriginals.getOrPut(normalized) { mutableListOf() }.add(original)
                normalized
            }

        normalizedToOriginals.forEach { (normalized, originals) ->
            if (originals.size > 1) {
                throw JavaEnumLiteralCollision(
                    schemaName = schemaName,
                    originals = originals,
                    normalized = normalized,
                    packageName = packageName,
                )
            }
        }

        return normalizedValues
    }

    private fun Any?.toEnumLiteral(): String =
        this
            .toString()
            .trimStart('"', '\'', '|', '>')
            .removeSurrounding("\"")

    private fun normalizeEnumLiteral(value: String): String = value.uppercase()
}
