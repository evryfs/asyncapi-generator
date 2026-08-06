package dev.banking.asyncapi.generator.core.generator.java.factory

import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.model.EnumLiteralNormalizer
import dev.banking.asyncapi.generator.core.generator.schema.isOpenPayload
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
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
                    values = enumLiteralNormalizer.normalize(name, schema.enum),
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
                    classAnnotations = annotation?.let { listOf("@${it.simpleName}") }.orEmpty(),
                    classAnnotationImports = annotation?.let { listOf(it.value) }.orEmpty(),
                )
            }

            else -> null
        }
    }
}
