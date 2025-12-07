package dev.banking.asyncapi.generator.core.generator.kotlin.factory

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.kotlin.mapper.ConstraintMapper
import dev.banking.asyncapi.generator.core.generator.kotlin.mapper.KotlinTypeMapper
import dev.banking.asyncapi.generator.core.generator.kotlin.model.PropertyModel
import dev.banking.asyncapi.generator.core.generator.kotlin.serialization.SerializationAnnotationMapper
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.isTypeNullable
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class PropertyFactory(
    val context: GeneratorContext,
    val serializationFramework: String = "jackson"
) {

    private val constraintMapper = ConstraintMapper()
    private val serializationAnnotationMapper = SerializationAnnotationMapper(serializationFramework)
    private val defaultValueFactory = DefaultValueFactory(context)
    private val validationDetector = ValidationDetector(context)
    private val typeMapper = KotlinTypeMapper(context)

    fun createProperty(
        propertyName: String,
        propSchemaInterface: SchemaInterface,
        requiredProperties: List<String>
    ): PropertyModel {

        // 1. Resolve Type & Schema
        val (finalPropSchema, baseKotlinType) = resolveTypeAndSchema(propertyName, propSchemaInterface)

        // 2. Determine Nullability
        val isExplicitlyNullableFromSchema = finalPropSchema?.let { it.nullable == true || it.type.isTypeNullable() } ?: false
        val isRequiredByParent = requiredProperties.contains(propertyName)
        val isNullable = !isRequiredByParent || isExplicitlyNullableFromSchema

        // 3. Build Annotations
        val annotations = mutableListOf<String>()
        annotations.addAll(constraintMapper.buildAnnotations(finalPropSchema))
        annotations.addAll(serializationAnnotationMapper.buildAnnotations(propertyName, finalPropSchema))

        if (validationDetector.needsCascadedValidation(baseKotlinType)) {
            annotations.add("@field:Valid")
        }

        // 4. Calculate Default Value
        val defaultValue = if (finalPropSchema != null) {
            defaultValueFactory.createDefaultValue(finalPropSchema, baseKotlinType, isNullable)
        } else {
            if (isNullable) "null" else null
        }

        val description = DocumentationUtils.toKDocLines(finalPropSchema?.description)

        return PropertyModel(
            name = propertyName,
            description = description,
            typeName = if (isNullable) "$baseKotlinType?" else baseKotlinType,
            defaultValue = defaultValue,
            annotations = annotations
        )
    }

    private fun resolveTypeAndSchema(
        propertyName: String,
        propSchemaInterface: SchemaInterface
    ): Pair<Schema?, String> {
        return when (propSchemaInterface) {
            is SchemaInterface.SchemaInline -> {
                val schema = propSchemaInterface.schema
                val type = typeMapper.mapKotlinType(propertyName, schema)
                schema to type
            }
            is SchemaInterface.SchemaReference -> {
                val type = typeMapper.typeNameFromRef(propSchemaInterface.reference)
                val schema = context.findSchemaByName(type)
                schema to type
            }
            is SchemaInterface.BooleanSchema -> {
                null to "Boolean"
            }
            else -> {
                null to "Any"
            }
        }
    }
}
