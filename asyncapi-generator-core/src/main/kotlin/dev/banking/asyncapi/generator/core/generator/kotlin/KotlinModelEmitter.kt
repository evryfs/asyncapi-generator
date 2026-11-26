package dev.banking.asyncapi.generator.core.generator.kotlin

import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.jvm.ConstraintAnnotationMapper
import dev.banking.asyncapi.generator.core.generator.jvm.DocComments
import dev.banking.asyncapi.generator.core.generator.jvm.JsonPropertyAccessAnnotationMapper
import dev.banking.asyncapi.generator.core.generator.jvm.SourceAnnotation
import dev.banking.asyncapi.generator.core.generator.jvm.SourceLiteralEscaper
import dev.banking.asyncapi.generator.core.generator.jvm.imports
import dev.banking.asyncapi.generator.core.generator.kotlin.model.KotlinClassTemplate
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.output.TemplateRenderer
import dev.banking.asyncapi.generator.core.generator.sourcemodel.DefaultValue
import dev.banking.asyncapi.generator.core.generator.sourcemodel.EnumModel
import dev.banking.asyncapi.generator.core.generator.sourcemodel.ModelDefinition
import dev.banking.asyncapi.generator.core.generator.sourcemodel.ModelEmitter
import dev.banking.asyncapi.generator.core.generator.sourcemodel.ObjectModel
import dev.banking.asyncapi.generator.core.generator.sourcemodel.OpenObjectModel
import dev.banking.asyncapi.generator.core.generator.sourcemodel.PropertyDefinition
import dev.banking.asyncapi.generator.core.generator.sourcemodel.TypeRef
import dev.banking.asyncapi.generator.core.generator.sourcemodel.UnionModel
import dev.banking.asyncapi.generator.core.generator.sourcemodel.cascadesValidation
import dev.banking.asyncapi.generator.core.generator.sourcemodel.rejectReservedNames

/**
 * Renders payload models as Kotlin sources: data classes, enums, sealed interfaces for unions,
 * and `Any` type aliases for open objects.
 *
 * @param annotation Annotation added to every data class; null for none.
 */
internal class KotlinModelEmitter(
    private val annotation: QualifiedTypeName? = null,
) : ModelEmitter {
    private val templates = TemplateRenderer("kotlin", GeneratedArtifactKind.SOURCE)

    override fun emit(models: List<ModelDefinition>): GenerationResult {
        models.rejectReservedNames("Kotlin", reservedTypeNames + listOfNotNull(annotation?.simpleName))
        return GenerationResult(
            models.map { model ->
                when (model) {
                    is ObjectModel -> render(model, "dataClass.mustache", dataClass(model))
                    is EnumModel -> render(model, "enum.mustache", enumClass(model))
                    is UnionModel -> render(model, "sealedInterface.mustache", sealedInterface(model))
                    is OpenObjectModel -> render(model, "typeAlias.mustache", typeAlias(model))
                }
            },
        )
    }

    private fun dataClass(model: ObjectModel): KotlinClassTemplate {
        val properties = model.properties.map(::property)
        return KotlinClassTemplate(
            packageName = model.packageName,
            className = model.name,
            classDocLines = DocComments.toKDocLines(model.description),
            fields =
                properties.withIndex().map { (index, property) ->
                    property.templateFields(index == properties.lastIndex)
                },
            imports = imports(model, properties),
            implementsClause =
                model.unions
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(", ", prefix = " : ")
                    .orEmpty(),
            classAnnotations =
                listOfNotNull(annotation?.let { "@${it.simpleName}" }) +
                    // Jackson refuses to serialize a bean without properties unless the class carries an
                    // annotation of its own.
                    listOf("@JsonIgnoreProperties(ignoreUnknown = true)").filter { properties.isEmpty() },
        )
    }

    private fun imports(
        model: ObjectModel,
        properties: List<KotlinProperty>,
    ): List<String> =
        (
            model.properties.flatMap { it.type.imports(model.packageName, model.name) } +
                properties.flatMap { it.annotationImports } +
                listOfNotNull(annotation?.value) +
                listOf("com.fasterxml.jackson.annotation.JsonIgnoreProperties").filter { properties.isEmpty() }
        ).distinct()
            .sorted()

    private fun property(property: PropertyDefinition): KotlinProperty {
        val wireName = property.wireName.takeIf { it != property.identifier }?.let(SourceLiteralEscaper::forKotlin)
        val jsonProperty = JsonPropertyAccessAnnotationMapper.annotationFor(property.access, wireName)
        // Annotations go on the backing field; without a wire name, @JsonProperty keeps Kotlin's
        // default target, the constructor parameter.
        val annotations =
            buildList {
                if (wireName != null) jsonProperty?.let { add(it to FIELD) }
                ConstraintAnnotationMapper
                    .buildAnnotations(property.constraints, SourceLiteralEscaper::forKotlin)
                    .forEach { add(it to FIELD) }
                if (wireName == null) jsonProperty?.let { add(it to null) }
                if (property.type.cascadesValidation) add(VALID to FIELD)
            }
        val type = typeName(property.type)
        val nullable = !property.required || property.nullable
        return KotlinProperty(
            name = property.identifier,
            type = type,
            description = DocComments.toKDocLines(property.description),
            nullable = nullable,
            defaultValue = property.default?.let { literal(it, type) } ?: "null".takeIf { nullable },
            annotations = annotations.map { (annotation, target) -> annotation.render(target) },
            annotationImports = annotations.flatMap { (annotation, _) -> annotation.imports },
        )
    }

    private fun enumClass(model: EnumModel) =
        mapOf(
            "packageName" to model.packageName,
            "name" to model.name,
            "hasDescription" to DocComments.toKDocLines(model.description).isNotEmpty(),
            "description" to DocComments.toKDocLines(model.description),
            "hasJsonValues" to model.constants.any { it.jsonValue != null },
            "values" to model.constants.map { it.copy(jsonValue = it.jsonValue?.let(SourceLiteralEscaper::forKotlin)) },
        )

    private fun sealedInterface(model: UnionModel) =
        mapOf(
            "packageName" to model.packageName,
            "interfaceName" to model.name,
            "hasDescription" to DocComments.toKDocLines(model.description).isNotEmpty(),
            "description" to DocComments.toKDocLines(model.description),
            "discriminator" to model.discriminator?.let(SourceLiteralEscaper::forKotlin),
            "deduction" to model.deduction,
            "hasTypeInfo" to (model.discriminator != null || model.deduction),
            "hasDiscriminator" to (model.discriminator != null),
            "hasSubTypes" to model.subtypes.isNotEmpty(),
            "subTypes" to model.subtypes.map { it.copy(name = SourceLiteralEscaper.forKotlin(it.name)) },
        )

    private fun typeAlias(model: OpenObjectModel) =
        mapOf(
            "packageName" to model.packageName,
            "name" to model.name,
            "aliasType" to typeName(TypeRef.Scalar.ANY),
            "imports" to emptyList<String>(),
        )

    private fun render(
        model: ModelDefinition,
        templateName: String,
        data: Any,
    ): GeneratedArtifact = templates.render(model.packageName, "${model.name}.kt", templateName, data)

    private data class KotlinProperty(
        val name: String,
        val type: String,
        val description: List<String>,
        val nullable: Boolean,
        val defaultValue: String?,
        val annotations: List<String>,
        val annotationImports: List<String>,
    ) {
        fun templateFields(last: Boolean): Map<String, Any?> =
            mapOf(
                "name" to name,
                "type" to type,
                "docFirstLine" to description.firstOrNull(),
                "docTailLines" to description.drop(1),
                "nullable" to nullable,
                "defaultValue" to defaultValue,
                "last" to last,
                "annotations" to annotations,
            )
    }

    companion object {
        private const val FIELD = "field"
        private val VALID = SourceAnnotation("jakarta.validation.Valid")

        /**
         * Types that generated Kotlin models use without qualification: the scalar, collection, and
         * date types, and the Jackson and Jakarta Validation annotations.
         */
        private val reservedTypeNames =
            setOf(
                "String",
                "Int",
                "Long",
                "Float",
                "Double",
                "Boolean",
                "Any",
                "List",
                "Map",
                "UUID",
                "LocalDate",
                "LocalTime",
                "OffsetDateTime",
                "BigDecimal",
                "JsonProperty",
                "JsonIgnoreProperties",
                "JsonTypeInfo",
                "JsonSubTypes",
                "Access",
                "Valid",
                "Size",
                "Pattern",
                "Min",
                "Max",
                "DecimalMin",
                "DecimalMax",
                "Email",
            )

        /** The Kotlin source name of [type]. */
        internal fun typeName(type: TypeRef): String =
            when (type) {
                is TypeRef.Scalar ->
                    when (type) {
                        TypeRef.Scalar.STRING -> "String"
                        TypeRef.Scalar.UUID -> "UUID"
                        TypeRef.Scalar.DATE -> "LocalDate"
                        TypeRef.Scalar.TIME -> "LocalTime"
                        TypeRef.Scalar.DATE_TIME -> "OffsetDateTime"
                        TypeRef.Scalar.INT32 -> "Int"
                        TypeRef.Scalar.INT64 -> "Long"
                        TypeRef.Scalar.FLOAT -> "Float"
                        TypeRef.Scalar.DOUBLE -> "Double"
                        TypeRef.Scalar.DECIMAL -> "BigDecimal"
                        TypeRef.Scalar.BOOLEAN -> "Boolean"
                        TypeRef.Scalar.ANY -> "Any"
                    }
                is TypeRef.ListOf -> "List<${typeName(type.element)}>"
                is TypeRef.MapOf -> "Map<String, ${typeName(type.value)}>"
                is TypeRef.Named -> type.name
                is TypeRef.External -> type.name
            }

        /** A Kotlin literal of [value] for a property of Kotlin type [type]. */
        private fun literal(
            value: DefaultValue,
            type: String,
        ): String =
            when (value) {
                is DefaultValue.EnumEntry -> "${value.enumName}.${value.constant}"
                is DefaultValue.BooleanLiteral -> value.value
                is DefaultValue.IntegerLiteral -> value.value
                is DefaultValue.NumberLiteral -> numberLiteral(value.value, type)
                is DefaultValue.StringLiteral -> "\"${SourceLiteralEscaper.forKotlin(value.value)}\""
                is DefaultValue.ParsedScalar -> parsedLiteral(value)
            }

        /** A call that parses the default text into the property's date, time, or UUID type. */
        private fun parsedLiteral(value: DefaultValue.ParsedScalar): String {
            val text = "\"${SourceLiteralEscaper.forKotlin(value.value)}\""
            return when (value.scalar) {
                TypeRef.Scalar.UUID -> "UUID.fromString($text)"
                else -> "${typeName(value.scalar)}.parse($text)"
            }
        }

        /** A number literal of the property's type: `1` is an Int literal, which a Double property rejects. */
        private fun numberLiteral(
            value: String,
            type: String,
        ): String {
            val decimal = if (value.contains('.') || value.contains('e', ignoreCase = true)) value else "$value.0"
            return when (type) {
                "Double" -> decimal
                "Float" -> "${decimal}f"
                "BigDecimal" -> "BigDecimal(\"$value\")"
                else -> value
            }
        }
    }
}
