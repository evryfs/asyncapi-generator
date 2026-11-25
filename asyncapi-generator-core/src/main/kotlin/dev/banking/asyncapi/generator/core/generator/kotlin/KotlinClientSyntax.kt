package dev.banking.asyncapi.generator.core.generator.kotlin

import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.jvm.ConstraintAnnotationMapper
import dev.banking.asyncapi.generator.core.generator.jvm.DocComments
import dev.banking.asyncapi.generator.core.generator.jvm.SourceLiteralEscaper
import dev.banking.asyncapi.generator.core.generator.jvm.imports
import dev.banking.asyncapi.generator.core.generator.kafka.spring.client.SpringKafkaClientSyntax
import dev.banking.asyncapi.generator.core.generator.sourcemodel.Constraints
import dev.banking.asyncapi.generator.core.generator.sourcemodel.TypeRef

/** Kotlin syntax of Spring Kafka clients: nullable types instead of nullability annotations. */
internal object KotlinClientSyntax : SpringKafkaClientSyntax {
    private val constraintMapper = ConstraintAnnotationMapper(SourceLanguage.KOTLIN)

    override val templateRoot = "kotlin"
    override val fileExtension = "kt"
    override val anyType = "Any?"
    override val byteArrayType = "ByteArray"

    override fun typeName(
        type: TypeRef,
        nullable: Boolean,
    ): String = KotlinModelEmitter.typeName(type) + if (nullable) "?" else ""

    override fun imports(
        type: TypeRef,
        modelPackage: String,
    ): List<String> = type.imports(modelPackage, "")

    override fun docLines(text: String?): List<String> = DocComments.toKDocLines(text)

    override fun code(text: String): String = "`$text`"

    override fun stringLiteral(value: String): String = SourceLiteralEscaper.forKotlin(value)

    override fun nullabilityAnnotation(nullable: Boolean): String? = null

    override fun parameterConstraints(constraints: Constraints): List<String> =
        constraintMapper.buildAnnotations(constraints).map { annotation -> annotation.replace("@field:", "@") }
}
