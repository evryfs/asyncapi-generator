package dev.banking.asyncapi.generator.core.generator.kotlin.factory

import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.model.EnumLiteralNormalizer
import dev.banking.asyncapi.generator.core.generator.schema.isOpenPayload
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema

class KotlinGeneratorModelFactory(
    val packageName: String,
    val context: GeneratorContext,
    val polymorphicRelationships: Map<String, List<String>>,
    val annotation: QualifiedTypeName? = null,
) {
    private val propertyFactory = PropertyFactory(context)
    private val enumLiteralNormalizer = EnumLiteralNormalizer(packageName)

    fun create(
        name: String,
        schema: Schema,
    ): GeneratorItem? {
        val isUnionType = !schema.oneOf.isNullOrEmpty() || !schema.anyOf.isNullOrEmpty()
        val isEnum = schema.type.getPrimaryType() == "string" && !schema.enum.isNullOrEmpty()
        val isObject = schema.type.getPrimaryType() == "object"
        val isOpenPayload = schema.isOpenPayload()

        val description = DocumentationUtils.toKDocLines(schema.description)

        return when {
            isEnum ->
                GeneratorItem.EnumClassModel(
                    name = name,
                    packageName = packageName,
                    description = description,
                    values = enumLiteralNormalizer.normalize(name, schema.enum),
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
                    if (annotation == null) {
                        emptyList<String>() to emptyList()
                    } else {
                        listOf("@${annotation.simpleName}") to listOf(annotation.value)
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
}
