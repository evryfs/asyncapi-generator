package dev.banking.asyncapi.generator.core.generator.kotlin

import dev.banking.asyncapi.generator.core.generator.ir.DefaultValue
import dev.banking.asyncapi.generator.core.generator.ir.EnumModel
import dev.banking.asyncapi.generator.core.generator.ir.ModelDefinition
import dev.banking.asyncapi.generator.core.generator.ir.ModelEmitter
import dev.banking.asyncapi.generator.core.generator.ir.ObjectModel
import dev.banking.asyncapi.generator.core.generator.ir.OpenObjectModel
import dev.banking.asyncapi.generator.core.generator.ir.PropertyDefinition
import dev.banking.asyncapi.generator.core.generator.ir.TypeRef
import dev.banking.asyncapi.generator.core.generator.ir.UnionModel
import dev.banking.asyncapi.generator.core.generator.ir.annotationImports
import dev.banking.asyncapi.generator.core.generator.ir.cascadesValidation
import dev.banking.asyncapi.generator.core.generator.ir.imports
import dev.banking.asyncapi.generator.core.generator.kotlin.model.KotlinClassTemplate
import dev.banking.asyncapi.generator.core.generator.model.ConstraintAnnotationMapper
import dev.banking.asyncapi.generator.core.generator.model.JsonPropertyAccessAnnotationMapper
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.util.DocumentationUtils
import dev.banking.asyncapi.generator.core.generator.util.SourceLiteralEscaper
import dev.banking.asyncapi.generator.core.generator.util.SourceMustacheFactory
import java.io.StringWriter

/**
 * Renders payload models as Kotlin sources: data classes, enums, sealed interfaces for unions,
 * and `Any` type aliases for open objects.
 */
internal class KotlinModelEmitter : ModelEmitter {
    private val mustacheFactory = SourceMustacheFactory("kotlin")
    private val constraintMapper = ConstraintAnnotationMapper(SourceLanguage.KOTLIN)

    override fun emit(models: List<ModelDefinition>): GenerationResult =
        GenerationResult(
            models.map { model ->
                when (model) {
                    is ObjectModel -> render(model, "dataClass.mustache", dataClass(model))
                    is EnumModel -> render(model, "enum.mustache", enumClass(model))
                    is UnionModel -> render(model, "sealedInterface.mustache", sealedInterface(model))
                    is OpenObjectModel -> render(model, "typeAlias.mustache", typeAlias(model))
                }
            },
        )

    private fun dataClass(model: ObjectModel): KotlinClassTemplate {
        val properties = model.properties.map(::property)
        val annotationImports = properties.flatMap { annotationImports(it.annotations, "@field:", "Access.") }
        val typeImports = model.properties.flatMap { it.type.imports(model.packageName, model.name) }
        return KotlinClassTemplate(
            packageName = model.packageName,
            className = model.name,
            classDocLines = DocumentationUtils.toKDocLines(model.description),
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
            imports = (typeImports + annotationImports + listOfNotNull(model.annotation?.value)).distinct().sorted(),
            implementsClause = model.unions.takeIf { it.isNotEmpty() }?.joinToString(", ", prefix = " : ").orEmpty(),
            classAnnotations = listOfNotNull(model.annotation?.let { "@${it.simpleName}" }),
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
            description = DocumentationUtils.toKDocLines(property.description),
            nullable = nullable,
            defaultValue = property.default?.let { literal(it, type) } ?: "null".takeIf { nullable },
            annotations = annotations,
        )
    }

    private fun enumClass(model: EnumModel) =
        mapOf(
            "packageName" to model.packageName,
            "name" to model.name,
            "hasJsonValues" to model.constants.any { it.jsonValue != null },
            "values" to model.constants.map { it.copy(jsonValue = it.jsonValue?.let(SourceLiteralEscaper::forKotlin)) },
        )

    private fun sealedInterface(model: UnionModel) =
        mapOf(
            "packageName" to model.packageName,
            "interfaceName" to model.name,
            "discriminator" to model.discriminator?.let(SourceLiteralEscaper::forKotlin),
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

    private companion object {
        /** The Kotlin source name of [type]. */
        private fun typeName(type: TypeRef): String =
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
