package dev.banking.asyncapi.generator.core.generator.kotlin.factory

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class KotlinGeneratorModelFactory(
    val packageName: String,
    val context: GeneratorContext,
    val polymorphicRelationships: Map<String, List<String>>,
    val annotation: String? = null,
) {
    private val propertyFactory = PropertyFactory(context)

    fun create(
        name: String,
        schema: Schema,
    ): GeneratorItem? {
        val isUnionType = !schema.oneOf.isNullOrEmpty() || !schema.anyOf.isNullOrEmpty()
        val isEnum = schema.type.getPrimaryType() == "string" && !schema.enum.isNullOrEmpty()
        val isObject = schema.type.getPrimaryType() == "object"
        val isOpenPayload = isOpenPayloadSchema(schema)

        val description = DocumentationUtils.toKDocLines(schema.description)

        return when {
            isEnum ->
                GeneratorItem.EnumClassModel(
                    name = name,
                    packageName = packageName,
                    description = description,
                    values =
                        schema.enum.map {
                            it
                                .toString()
                                .trimStart('"', '\'', '|', '>')
                                .removeSurrounding("\"")
                                .uppercase()
                        },
                )
            isUnionType ->
                GeneratorItem.SealedInterfaceModel(
                    name = name,
                    packageName = packageName,
                    description = description,
                )
            isOpenPayload ->
                GeneratorItem.TypeAliasModel(
                    name = name,
                    packageName = packageName,
                    description = description,
                    aliasType = "Any",
                )
            isObject -> {
                val properties =
                    schema.properties?.map { (propName, propSchema) ->
                        propertyFactory.createProperty(propName, propSchema, schema.required ?: emptyList())
                    } ?: emptyList()
                val (classAnnotations, classAnnotationImports) =
                    if (annotation.isNullOrBlank()) {
                        emptyList<String>() to emptyList()
                    } else {
                        val shortName = annotation.substringAfterLast(".")
                        listOf("@$shortName") to listOf(annotation)
                    }
                GeneratorItem.DataClassModel(
                    name = name,
                    packageName = packageName,
                    description = description,
                    properties = properties,
                    parentInterfaces = polymorphicRelationships[name] ?: emptyList(),
                    classAnnotations = classAnnotations,
                    classAnnotationImports = classAnnotationImports,
                )
            }
            else -> null // This schema type does not result in its own generated file (e.g., a primitive type alias)
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
}
