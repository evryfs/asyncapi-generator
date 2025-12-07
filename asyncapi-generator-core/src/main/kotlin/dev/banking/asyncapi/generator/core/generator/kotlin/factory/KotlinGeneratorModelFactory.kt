package dev.banking.asyncapi.generator.core.generator.kotlin.factory

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema

class KotlinGeneratorModelFactory(
    val packageName: String,
    val context: GeneratorContext,
    val polymorphicRelationships: Map<String, List<String>>,
) {
    private val propertyFactory = PropertyFactory(context)

    fun create(name: String, schema: Schema): GeneratorItem? {
        val isUnionType = !schema.oneOf.isNullOrEmpty() || !schema.anyOf.isNullOrEmpty()
        val isEnum = schema.type.getPrimaryType() == "string" && !schema.enum.isNullOrEmpty()
        val isObject = schema.type.getPrimaryType() == "object"

        val description = DocumentationUtils.toKDocLines(schema.description)

        return when {
            isEnum -> GeneratorItem.EnumClassModel(
                name = name,
                packageName = packageName,
                description = description,
                values = schema.enum.map {
                    it.toString().trimStart('"', '\'', '|', '>').removeSurrounding("\"").uppercase()
                }
            )
            isUnionType -> GeneratorItem.SealedInterfaceModel(
                name = name,
                packageName = packageName,
                description = description
            )
            isObject -> {
                val properties = schema.properties?.map { (propName, propSchema) ->
                    propertyFactory.createProperty(propName, propSchema, schema.required ?: emptyList())
                } ?: emptyList()
                GeneratorItem.DataClassModel(
                    name = name,
                    packageName = packageName,
                    description = description,
                    properties = properties,
                    parentInterfaces = polymorphicRelationships[name] ?: emptyList()
                )
            }
            else -> null // This schema type does not result in its own generated file (e.g., a primitive type alias)
        }
    }
}
