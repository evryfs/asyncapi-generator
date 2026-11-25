package dev.banking.asyncapi.generator.core.generator.java

import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.ir.Constraints
import dev.banking.asyncapi.generator.core.generator.ir.TypeRef
import dev.banking.asyncapi.generator.core.generator.java.JavaModelEmitter.Companion.withoutOpenObjects
import dev.banking.asyncapi.generator.core.generator.jvm.ConstraintAnnotationMapper
import dev.banking.asyncapi.generator.core.generator.jvm.DocComments
import dev.banking.asyncapi.generator.core.generator.jvm.SourceLiteralEscaper
import dev.banking.asyncapi.generator.core.generator.jvm.imports
import dev.banking.asyncapi.generator.core.generator.kafka.spring.client.SpringKafkaClientSyntax

/** Java syntax of Spring Kafka clients: nullability annotations, and `Object` for open objects. */
internal object JavaClientSyntax : SpringKafkaClientSyntax {
    private val constraintMapper = ConstraintAnnotationMapper(SourceLanguage.JAVA)

    override val templateRoot = "java"
    override val fileExtension = "java"
    override val anyType = "Object"
    override val byteArrayType = "byte[]"

    override fun typeName(
        type: TypeRef,
        nullable: Boolean,
    ): String = JavaModelEmitter.typeName(type)

    override fun imports(
        type: TypeRef,
        modelPackage: String,
    ): List<String> = type.withoutOpenObjects().imports(modelPackage, "", listImport = "java.util.List")

    override fun docLines(text: String?): List<String> = DocComments.toJavaDocLines(text)

    override fun code(text: String): String = "{@code $text}"

    override fun stringLiteral(value: String): String = SourceLiteralEscaper.forJava(value)

    override fun nullabilityAnnotation(nullable: Boolean): String = if (nullable) "@Nullable" else "@NotNull"

    override fun parameterConstraints(constraints: Constraints): List<String> =
        constraintMapper.buildAnnotations(constraints)
}
