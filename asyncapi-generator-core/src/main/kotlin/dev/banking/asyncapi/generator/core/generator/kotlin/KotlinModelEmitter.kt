package dev.banking.asyncapi.generator.core.generator.kotlin

import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.ir.DefaultValue
import dev.banking.asyncapi.generator.core.generator.ir.EnumModel
import dev.banking.asyncapi.generator.core.generator.ir.ModelDefinition
import dev.banking.asyncapi.generator.core.generator.ir.ModelEmitter
import dev.banking.asyncapi.generator.core.generator.ir.ObjectModel
import dev.banking.asyncapi.generator.core.generator.ir.OpenObjectModel
import dev.banking.asyncapi.generator.core.generator.ir.PropertyDefinition
import dev.banking.asyncapi.generator.core.generator.ir.TypeRef
import dev.banking.asyncapi.generator.core.generator.ir.UnionModel
import dev.banking.asyncapi.generator.core.generator.ir.cascadesValidation
import dev.banking.asyncapi.generator.core.generator.ir.rejectReservedNames
import dev.banking.asyncapi.generator.core.generator.jvm.ConstraintAnnotationMapper
import dev.banking.asyncapi.generator.core.generator.jvm.DocComments
import dev.banking.asyncapi.generator.core.generator.jvm.JsonPropertyAccessAnnotationMapper
import dev.banking.asyncapi.generator.core.generator.jvm.SourceLiteralEscaper
import dev.banking.asyncapi.generator.core.generator.jvm.SourceMustacheFactory
import dev.banking.asyncapi.generator.core.generator.jvm.annotationImports
import dev.banking.asyncapi.generator.core.generator.jvm.imports
import dev.banking.asyncapi.generator.core.generator.kotlin.model.KotlinClassTemplate
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import java.io.StringWriter

/**
 * Renders payload models as Kotlin sources: data classes, enums, sealed interfaces for unions,
 * and `Any` type aliases for open objects.
 *
 * @param annotation Annotation added to every data class; null for none.
 */
internal class KotlinModelEmitter(
    private val annotation: QualifiedTypeName? = null,
) : ModelEmitter {
    private val mustacheFactory = SourceMustacheFactory("kotlin")
    private val constraintMapper = ConstraintAnnotationMapper(SourceLanguage.KOTLIN)

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
        val annotationImports = properties.flatMap { annotationImports(it.annotations, "@field:", "Access.") }
        // Jackson refuses to serialize a bean without properties unless the class carries an annotation of its own.
        val emptyClassImports =
            if (properties.isEmpty()) {
                listOf(
                    "com.fasterxml.jackson.annotation.JsonIgnoreProperties",
                )
            } else {
                emptyList()
            }
        val emptyClassAnnotations =
            if (properties.isEmpty()) {
                listOf(
                    "@JsonIgnoreProperties(ignoreUnknown = true)",
                )
            } else {
                emptyList()
            }
        val typeImports = model.properties.flatMap { it.type.imports(model.packageName, model.name) }
        return KotlinClassTemplate(
            packageName = model.packageName,
            className = model.name,
            classDocLines = DocComments.toKDocLines(model.description),
            fields =
                properties.mapIndexed { index, property ->
                    mapOf(
                        "name" to property.name,
                        "type" to property.type,
                        "docFirstLine" to property.description.firstOrNull(),
                        "docTailLines" to property.description.drop(1),
                        "nullable" to property.nullable,
                        "defaultValue" to property.defaultValue,
                        "last" to (index == properties.lastIndex),
                        "annotations" to property.annotations,
                    )
                },
            imports =
                (typeImports + annotationImports + listOfNotNull(annotation?.value) + emptyClassImports)
                    .distinct()
                    .sorted(),
            implementsClause =
                model.unions
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(", ", prefix = " : ")
                    .orEmpty(),
            classAnnotations = listOfNotNull(annotation?.let { "@${it.simpleName}" }) + emptyClassAnnotations,
        )
    }

    private fun property(property: PropertyDefinition): KotlinProperty {
        val schema = property.schema
        val wireName = property.wireName.takeIf { it != property.identifier }?.let(SourceLiteralEscaper::forKotlin)
        val jsonProperty = JsonPropertyAccessAnnotationMapper.annotationFor(schema, wireName, "@field:JsonProperty")
        val annotations =
            buildList {
                if (wireName != null) jsonProperty?.let(::add)
                addAll(constraintMapper.buildAnnotations(schema))
                if (wireName == null) jsonProperty?.let(::add)
                if (property.type.cascadesValidation) add("@field:Valid")
            }
        val type = typeName(property.type)
        val nullable = !property.required || property.nullableInSchema
        return KotlinProperty(
            name = property.identifier,
            type = type,
            description = DocComments.toKDocLines(property.description),
            nullable = nullable,
            defaultValue = property.default?.let { literal(it, type) } ?: "null".takeIf { nullable },
            annotations = annotations,
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
    ): GeneratedArtifact {
        val writer = StringWriter()
        mustacheFactory.compile(templateName).execute(writer, data).flush()
        return GeneratedArtifact(
            relativePath = GeneratedArtifactPaths.fromNamespace(model.packageName, "${model.name}.kt"),
            content = writer.toString(),
            kind = GeneratedArtifactKind.SOURCE,
        )
    }

    private data class KotlinProperty(
        val name: String,
        val type: String,
        val description: List<String>,
        val nullable: Boolean,
        val defaultValue: String?,
        val annotations: List<String>,
    )

    companion object {
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
