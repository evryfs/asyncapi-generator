package dev.banking.asyncapi.generator.core.generator.java

import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType
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
import dev.banking.asyncapi.generator.core.generator.java.model.JavaClassTemplate
import dev.banking.asyncapi.generator.core.generator.java.model.JavaRecordTemplate
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
 * Renders payload models as Java sources: classes or records, enums, and interfaces for unions.
 *
 * Java has no type alias, so an open object gets no source and references to it are `Object`.
 */
internal class JavaModelEmitter(
    private val javaModelType: JavaModelType = JavaModelType.CLASS,
) : ModelEmitter {
    private val mustacheFactory = SourceMustacheFactory("java")
    private val constraintMapper = ConstraintAnnotationMapper(SourceLanguage.JAVA)

    override fun emit(models: List<ModelDefinition>): GenerationResult =
        GenerationResult(
            models.mapNotNull { model ->
                when (model) {
                    is ObjectModel ->
                        when (javaModelType) {
                            JavaModelType.CLASS -> render(model, "javaClass.mustache", javaClass(model))
                            JavaModelType.RECORD -> render(model, "javaRecord.mustache", javaRecord(model))
                        }
                    is EnumModel -> render(model, "javaEnum.mustache", javaEnum(model))
                    is UnionModel -> render(model, "javaInterface.mustache", javaInterface(model))
                    is OpenObjectModel -> null
                }
            },
        )

    private fun javaClass(model: ObjectModel): JavaClassTemplate {
        val properties = model.properties.map(::property)
        val fields =
            properties.mapIndexed { index, property ->
                property.templateFields(last = index == properties.lastIndex) +
                    mapOf("getterName" to property.accessorName("get"), "setterName" to property.accessorName("set"))
            }
        return JavaClassTemplate(
            packageName = model.packageName,
            className = model.name,
            classDocLines = DocumentationUtils.toJavaDocLines(model.description),
            fields = fields,
            allFields = fields,
            imports = imports(model, properties, "java.util.Objects"),
            implementsClause = implementsClause(model),
            classAnnotations = classAnnotations(model),
        )
    }

    private fun javaRecord(model: ObjectModel): JavaRecordTemplate {
        val properties = model.properties.map(::property)
        return JavaRecordTemplate(
            packageName = model.packageName,
            className = model.name,
            classDocLines = DocumentationUtils.toJavaDocLines(model.description),
            fields =
                properties.mapIndexed { index, property ->
                    val last = index == properties.lastIndex
                    property.templateFields(last) +
                        mapOf("componentSeparator" to if (last) "" else ",", "componentSpacer" to if (last) "" else "\n")
                },
            imports = imports(model, properties),
            implementsClause = implementsClause(model),
            classAnnotations = classAnnotations(model),
        )
    }

    private fun property(definition: PropertyDefinition): JavaProperty {
        // Java generates no type for an open object, so a reference to one has no schema to read.
        val property =
            if ((definition.type as? TypeRef.Named)?.isOpenObject == true) definition.copy(schema = null) else definition
        val type = property.type.withoutOpenObjects()
        val wireName = property.wireName.takeIf { it != property.identifier }?.let(SourceLiteralEscaper::forJava)
        val jsonProperty = JsonPropertyAccessAnnotationMapper.annotationFor(property.schema, wireName, "@JsonProperty")
        val annotations =
            buildList {
                if (wireName != null) jsonProperty?.let(::add)
                addAll(constraintMapper.buildAnnotations(property.schema))
                if (wireName == null) jsonProperty?.let(::add)
                if (property.required && !property.nullableInSchema) add("@NotNull")
                if (type.cascadesValidation) add("@Valid")
            }
        return JavaProperty(
            name = property.identifier,
            type = type,
            description = DocumentationUtils.toJavaDocLines(property.description),
            annotations = annotations,
        )
    }

    private fun imports(
        model: ObjectModel,
        properties: List<JavaProperty>,
        vararg fixedImports: String,
    ): List<String> {
        val typeImports =
            properties.flatMap { it.type.imports(model.packageName, model.name, "java.util.List", "java.util.Map") }
        val annotationImports = properties.flatMap { annotationImports(it.annotations, "@", "access = Access.") }
        return (fixedImports.toList() + "java.io.Serializable" + typeImports + annotationImports + listOfNotNull(model.annotation?.value))
            .distinct()
            .sorted()
    }

    private fun implementsClause(model: ObjectModel): String = " implements " + (model.unions + "Serializable").joinToString(", ")

    private fun classAnnotations(model: ObjectModel): List<String> = listOfNotNull(model.annotation?.let { "@${it.simpleName}" })

    private fun javaEnum(model: EnumModel) =
        mapOf(
            "packageName" to model.packageName,
            "name" to model.name,
            "description" to DocumentationUtils.toJavaDocLines(model.description),
            "hasJsonValues" to model.constants.any { it.jsonValue != null },
            "values" to model.constants.map { it.copy(jsonValue = it.jsonValue?.let(SourceLiteralEscaper::forJava)) },
        )

    private fun javaInterface(model: UnionModel) =
        mapOf(
            "packageName" to model.packageName,
            "interfaceName" to model.name,
            "description" to DocumentationUtils.toJavaDocLines(model.description),
            "discriminator" to model.discriminator,
            "hasSubTypes" to model.subtypes.isNotEmpty(),
            "subTypes" to
                model.subtypes.mapIndexed { index, subtype ->
                    mapOf("type" to subtype.type, "name" to subtype.name, "last" to (index == model.subtypes.lastIndex))
                },
        )

    private fun render(
        model: ModelDefinition,
        templateName: String,
        data: Any,
    ): GeneratedArtifact {
        val writer = StringWriter()
        mustacheFactory.compile(templateName).execute(writer, data).flush()
        return GeneratedArtifact(
            relativePath = GeneratedArtifactPaths.fromNamespace(model.packageName, "${model.name}.java"),
            content = writer.toString(),
            kind = GeneratedArtifactKind.SOURCE,
        )
    }

    private data class JavaProperty(
        val name: String,
        val type: TypeRef,
        val description: List<String>,
        val annotations: List<String>,
    ) {
        fun accessorName(prefix: String): String = prefix + name.replaceFirstChar { it.uppercase() }

        fun templateFields(last: Boolean): Map<String, Any?> =
            mapOf(
                "name" to name,
                "type" to typeName(type),
                "docFirstLine" to description.firstOrNull(),
                "docTailLines" to description.drop(1),
                "last" to last,
                "annotations" to annotations,
            )
    }

    private companion object {
        /** The Java source name of [type]. */
        private fun typeName(type: TypeRef): String =
            when (type) {
                is TypeRef.Scalar ->
                    when (type) {
                        TypeRef.Scalar.STRING -> "String"
                        TypeRef.Scalar.UUID -> "UUID"
                        TypeRef.Scalar.DATE -> "LocalDate"
                        TypeRef.Scalar.TIME -> "LocalTime"
                        TypeRef.Scalar.DATE_TIME -> "OffsetDateTime"
                        TypeRef.Scalar.INT32 -> "Integer"
                        TypeRef.Scalar.INT64 -> "Long"
                        TypeRef.Scalar.FLOAT -> "Float"
                        TypeRef.Scalar.DOUBLE -> "Double"
                        TypeRef.Scalar.DECIMAL -> "BigDecimal"
                        TypeRef.Scalar.BOOLEAN -> "Boolean"
                        TypeRef.Scalar.ANY -> "Object"
                    }
                is TypeRef.ListOf -> "List<${typeName(type.element)}>"
                is TypeRef.MapOf -> "Map<String, ${typeName(type.value)}>"
                is TypeRef.Named -> if (type.isOpenObject) "Object" else type.name
            }

        private fun TypeRef.withoutOpenObjects(): TypeRef =
            when (this) {
                is TypeRef.Named -> if (isOpenObject) TypeRef.Scalar.ANY else this
                is TypeRef.ListOf -> TypeRef.ListOf(element.withoutOpenObjects())
                is TypeRef.MapOf -> TypeRef.MapOf(value.withoutOpenObjects())
                is TypeRef.Scalar -> this
            }
    }
}
