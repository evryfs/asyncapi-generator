package dev.banking.asyncapi.generator.core.model.diagnostics

import dev.banking.asyncapi.generator.core.reader.SourceLocation
import kotlin.reflect.KClass
import kotlin.reflect.KType

enum class ParserDiagnosticCategory(val code: String) {
    MISSING_REQUIRED_MEMBER("parser.missing-required-member"),
    UNEXPECTED_VALUE_TYPE("parser.unexpected-value-type"),
}

sealed interface ParserDiagnostic {
    val category: ParserDiagnosticCategory
    val path: String
    val sourceLocation: SourceLocation

    data class MissingRequiredMember(
        val memberName: String,
        override val path: String,
        override val sourceLocation: SourceLocation,
    ) : ParserDiagnostic {
        override val category: ParserDiagnosticCategory =
            ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER
    }

    data class UnexpectedValueType(
        val expectedType: KType,
        val actualType: KClass<*>?,
        val actualValue: Any?,
        override val path: String,
        override val sourceLocation: SourceLocation,
    ) : ParserDiagnostic {
        override val category: ParserDiagnosticCategory =
            ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE
    }
}
