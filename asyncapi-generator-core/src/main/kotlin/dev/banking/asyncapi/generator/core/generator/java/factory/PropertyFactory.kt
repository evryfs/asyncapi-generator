package dev.banking.asyncapi.generator.core.generator.java.factory

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.generator.java.mapper.JavaTypeMapper
import dev.banking.asyncapi.generator.core.generator.java.model.PropertyModel
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
    private val typeMapper = JavaTypeMapper(context)
    private val constraintMapper = ConstraintAnnotationMapper(SourceLanguage.JAVA)

    private val defaultValueFactory = DefaultValueFactory(context)
    private val validationDetector = ValidationDetector(context)

    fun createProperty(
        propertyName: String,
        propSchemaInterface: SchemaInterface,
        requiredProperties: List<String>,
    ): PropertyModel {
        val (finalPropSchema, baseJavaType) = resolveTypeAndSchema(propertyName, propSchemaInterface)
        val identifier = SourceIdentifierMapper.toIdentifier(propertyName)

        val isRequired = requiredProperties.contains(propertyName)
        val isSchemaNullable = finalPropSchema?.type.isTypeNullable()

        val annotations = mutableListOf<String>()
        if (identifier != propertyName) {
            annotations.add("@JsonProperty(\"$propertyName\")")
        }
        annotations.addAll(constraintMapper.buildAnnotations(finalPropSchema))
        mergeJsonPropertyAnnotations(annotations)
        JsonPropertyAccessAnnotationMapper.annotationFor(finalPropSchema)?.let { accessAnnotation ->
            mergeJsonPropertyAnnotations(annotations, accessAnnotation)
        }

        if (isRequired && !isSchemaNullable) {
            annotations.add("@NotNull")
        }

        if (validationDetector.needsCascadedValidation(baseJavaType)) {
            annotations.add("@Valid")
        }

        val description = DocumentationUtils.toJavaDocLines(finalPropSchema?.description)
        val getterName = "get" + identifier.replaceFirstChar { it.uppercase() }
        val setterName = "set" + identifier.replaceFirstChar { it.uppercase() }

        return PropertyModel(
            name = identifier,
            description = description,
            typeName = baseJavaType,
            getterName = getterName,
            setterName = setterName,
            annotations = annotations,
        )
    }

    private fun mergeJsonPropertyAnnotations(
        annotations: MutableList<String>,
        additionalAnnotation: String? = null,
    ) {
        val jsonPropertyPrefix = "@JsonProperty"
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
                val type = typeMapper.mapJavaType(propertyName, schema)
                schema to type
            }
            is SchemaInterface.SchemaReference -> {
                val referencedTypeName = typeMapper.typeNameFromRef(propSchemaInterface.reference)
                val schema = context.findSchemaByName(referencedTypeName)
                val type =
                    if (schema?.isScalarAlias() == true) {
                        typeMapper.mapJavaType(propertyName, schema)
                    } else {
                        referencedTypeName
                    }
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
