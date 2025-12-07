package com.tietoevry.banking.asyncapi.generator.core.generator.java.factory

import com.tietoevry.banking.asyncapi.generator.core.generator.context.GeneratorContext
import com.tietoevry.banking.asyncapi.generator.core.generator.java.mapper.ConstraintMapper
import com.tietoevry.banking.asyncapi.generator.core.generator.java.mapper.JavaTypeMapper
import com.tietoevry.banking.asyncapi.generator.core.generator.java.model.PropertyModel
import com.tietoevry.banking.asyncapi.generator.core.generator.java.serialization.SerializationAnnotationMapper
import com.tietoevry.banking.asyncapi.generator.core.generator.util.DocumentationUtils
import com.tietoevry.banking.asyncapi.generator.core.generator.util.MapperUtil.isTypeNullable
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class PropertyFactory(
    val context: GeneratorContext,
    val serializationFramework: String = "jackson"
) {

    private val typeMapper = JavaTypeMapper(context)
    private val constraintMapper = ConstraintMapper()
    private val serializationAnnotationMapper = SerializationAnnotationMapper(serializationFramework)

    private val defaultValueFactory = DefaultValueFactory(context)
    private val validationDetector = ValidationDetector(context)

    fun createProperty(
        propertyName: String,
        propSchemaInterface: SchemaInterface,
        requiredProperties: List<String>
    ): PropertyModel {

        val (finalPropSchema, baseJavaType) = resolveTypeAndSchema(propertyName, propSchemaInterface)

        // In Java, primitives are nullable unless we use 'int' vs 'Integer'.
        // We default to Object wrappers (Integer) so everything is nullable by default.
        // If required, we add @NotNull.
        val isRequired = requiredProperties.contains(propertyName)
        // AsyncAPI "nullable" keyword
        val isSchemaNullable = finalPropSchema?.let { it.nullable == true || it.type.isTypeNullable() } ?: false

        val annotations = mutableListOf<String>()
        annotations.addAll(constraintMapper.buildAnnotations(finalPropSchema))
        // Pass propertyName to SerializationAnnotationMapper
        annotations.addAll(serializationAnnotationMapper.buildAnnotations(propertyName, finalPropSchema))

        if (isRequired && !isSchemaNullable) {
            annotations.add("@NotNull")
        }

        if (validationDetector.needsCascadedValidation(baseJavaType)) {
            annotations.add("@Valid")
        }

        val description = DocumentationUtils.toJavaDocLines(finalPropSchema?.description)
        val getterName = "get" + propertyName.replaceFirstChar { it.uppercase() }
        val setterName = "set" + propertyName.replaceFirstChar { it.uppercase() }

        return PropertyModel(
            name = propertyName,
            description = description,
            typeName = baseJavaType,
            getterName = getterName,
            setterName = setterName,
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
                val type = typeMapper.mapJavaType(propertyName, schema)
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
                null to "Object"
            }
        }
    }
}
