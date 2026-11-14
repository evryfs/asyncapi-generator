package dev.banking.asyncapi.generator.core.generator.java.factory

import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.model.EnumLiteralNormalizer
import dev.banking.asyncapi.generator.core.generator.model.UnionSubtypes
import dev.banking.asyncapi.generator.core.generator.schema.isOpenPayload
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.generator.util.SourceLiteralEscaper
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class JavaGeneratorModelFactory(
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

        val description = DocumentationUtils.toJavaDocLines(schema.description)

        return when {
            isEnum ->
                GeneratorItem.EnumModel(
                    name = name,
                    packageName = packageName,
                    description = description,
                    values = enumLiteralNormalizer.normalize(name, schema.enum).map { constant ->
                        GeneratorItem.EnumConstantModel(
                            name = constant.name,
                            jsonValue = constant.jsonValue?.let(SourceLiteralEscaper::forJava),
                        )
                    },
                )
            isUnionType -> {
                val discriminatorPropertyName = schema.discriminator
                val subTypes =
                    UnionSubtypes.of(schema, context).map { GeneratorItem.InterfaceModel.SubType(name = it.name, type = it.type) }

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
                    classAnnotations = annotation?.let { listOf("@${it.simpleName}") }.orEmpty(),
                    classAnnotationImports = annotation?.let { listOf(it.value) }.orEmpty(),
                )
            }

            else -> null
        }
    }
}
