package dev.banking.asyncapi.generator.core.generator.kafka.spring.client

import dev.banking.asyncapi.generator.core.generator.sourcemodel.Constraints
import dev.banking.asyncapi.generator.core.generator.sourcemodel.TypeRef

/** How one source language writes the parts of a Spring Kafka client that differ between languages. */
internal interface SpringKafkaClientSyntax {
    /** Folder of the language's client templates. */
    val templateRoot: String

    /** Extension of the generated source files. */
    val fileExtension: String

    /** Name of the language's top type. */
    val anyType: String

    /** Name of the byte array type. */
    val byteArrayType: String

    /** Source name of [type] for a parameter that accepts `null` when [nullable] is set. */
    fun typeName(
        type: TypeRef,
        nullable: Boolean = false,
    ): String

    /** Imports a client in another package than [modelPackage] needs to use [type]. */
    fun imports(
        type: TypeRef,
        modelPackage: String,
    ): List<String>

    /** Documentation comment lines of [text]. */
    fun docLines(text: String?): List<String>

    /** [text] marked as code in a documentation comment. */
    fun code(text: String): String

    /** [value] escaped for a string literal. */
    fun stringLiteral(value: String): String

    /** Annotation marking a parameter as nullable or not null; `null` where the type itself says so. */
    fun nullabilityAnnotation(nullable: Boolean): String?

    /** Jakarta Validation annotations that express [constraints] on a parameter. */
    fun parameterConstraints(constraints: Constraints): List<String>
}
