package dev.banking.asyncapi.generator.core.generator.kotlin.factory

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.kotlin.mapper.KotlinTypeMapper
import dev.banking.asyncapi.generator.core.generator.kotlin.model.PropertyModel
import dev.banking.asyncapi.generator.core.generator.model.ConstraintAnnotationMapper
import dev.banking.asyncapi.generator.core.generator.model.JsonPropertyAccessAnnotationMapper
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.schema.isScalarAlias
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.isTypeNullable
import dev.banking.asyncapi.generator.core.generator.util.SourceIdentifierMapper
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class PropertyFactory(
    val context: GeneratorContext,
) {
    private val constraintMapper = ConstraintAnnotationMapper(SourceLanguage.KOTLIN)
    private val defaultValueFactory = DefaultValueFactory(context)
    private val validationDetector = ValidationDetector(context)
    private val typeMapper = KotlinTypeMapper(context)

    fun createProperty(
        propertyName: String,
        propSchemaInterface: SchemaInterface,
        requiredProperties: List<String>,
    ): PropertyModel {
        val (finalPropSchema, baseKotlinType) = resolveTypeAndSchema(propertyName, propSchemaInterface)
        val identifier = SourceIdentifierMapper.toIdentifier(propertyName)

        val isExplicitlyNullableFromSchema = finalPropSchema?.type.isTypeNullable()
        val isRequiredByParent = requiredProperties.contains(propertyName)
        val isNullable = !isRequiredByParent || isExplicitlyNullableFromSchema

        val annotations = mutableListOf<String>()
        if (identifier != propertyName) {
            annotations.add("@field:JsonProperty(\"$propertyName\")")
        }
        annotations.addAll(constraintMapper.buildAnnotations(finalPropSchema))
        mergeJsonPropertyAnnotations(annotations)
        JsonPropertyAccessAnnotationMapper.annotationFor(finalPropSchema)?.let { accessAnnotation ->
            mergeJsonPropertyAnnotations(annotations, accessAnnotation)
        }

        if (validationDetector.needsCascadedValidation(baseKotlinType)) {
            annotations.add("@field:Valid")
        }
        val defaultValue =
            if (finalPropSchema != null) {
                defaultValueFactory.createDefaultValue(finalPropSchema, baseKotlinType, isNullable)
            } else {
                if (isNullable) "null" else null
            }
        val description = DocumentationUtils.toKDocLines(finalPropSchema?.description)

        return PropertyModel(
            name = identifier,
            description = description,
            typeName = if (isNullable) "$baseKotlinType?" else baseKotlinType,
            defaultValue = defaultValue,
            annotations = annotations,
        )
    }

    private fun mergeJsonPropertyAnnotations(
        annotations: MutableList<String>,
        additionalAnnotation: String? = null,
    ) {
        val jsonPropertyPrefix = "@field:JsonProperty"
        val existingIndex = annotations.indexOfFirst { it.startsWith(jsonPropertyPrefix) }
        if (existingIndex == -1) {
            additionalAnnotation?.let { annotations.add(it) }
            return
        }

        val existing = annotations[existingIndex]
        val valueMatch = Regex("""value\s*=\s*"([^"]+)""").find(existing)
        val accessMatch = Regex("""access\s*=\s*(\S+)""").find(existing)

        val value = valueMatch?.groupValues?.get(1)
        val access = accessMatch?.groupValues?.get(1)
            ?: additionalAnnotation?.let { Regex("""access\s*=\s*(\S+)""").find(it)?.groupValues?.get(1) }

        val parts = mutableListOf<String>()
        value?.let { parts.add("value = \"$it\"") }
        access?.let { parts.add("access = $it") }

        annotations[existingIndex] = if (parts.isEmpty()) {
            existing
        } else {
            "$jsonPropertyPrefix(${parts.joinToString(", ")})"
        }
    }

    private fun resolveTypeAndSchema(
        propertyName: String,
        propSchemaInterface: SchemaInterface,
    ): Pair<Schema?, String> =
        when (propSchemaInterface) {
            is SchemaInterface.SchemaInline -> {
                val schema = propSchemaInterface.schema
                val type = typeMapper.mapKotlinType(propertyName, schema)
                schema to type
            }
            is SchemaInterface.SchemaReference -> {
                val referencedTypeName = typeMapper.typeNameFromRef(propSchemaInterface.reference)
                val schema = context.findSchemaByName(referencedTypeName)
                val type =
                    if (schema?.isScalarAlias() == true) {
                        typeMapper.mapKotlinType(propertyName, schema)
                    } else {
                        referencedTypeName
                    }
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
