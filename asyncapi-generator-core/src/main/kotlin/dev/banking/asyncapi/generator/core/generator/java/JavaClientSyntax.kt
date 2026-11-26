package dev.banking.asyncapi.generator.core.generator.java

import dev.banking.asyncapi.generator.core.generator.java.JavaModelEmitter.Companion.withoutOpenObjects
import dev.banking.asyncapi.generator.core.generator.jvm.DocComments
import dev.banking.asyncapi.generator.core.generator.jvm.SourceAnnotation
import dev.banking.asyncapi.generator.core.generator.jvm.SourceLiteralEscaper
import dev.banking.asyncapi.generator.core.generator.jvm.imports
import dev.banking.asyncapi.generator.core.generator.kafka.spring.client.SpringKafkaClientSyntax
import dev.banking.asyncapi.generator.core.generator.sourcemodel.TypeRef

/** Java syntax of Spring Kafka clients: nullability annotations, and `Object` for open objects. */
internal object JavaClientSyntax : SpringKafkaClientSyntax {
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

    override fun nullabilityAnnotation(nullable: Boolean): SourceAnnotation = if (nullable) NULLABLE else NOT_NULL

    private val NULLABLE = SourceAnnotation("org.springframework.lang.Nullable")
    private val NOT_NULL = SourceAnnotation("jakarta.validation.constraints.NotNull")
}
