package dev.banking.asyncapi.generator.core.model.diagnostics

import dev.banking.asyncapi.generator.core.reader.SourceLocation

/** Stable categories for failures produced while mapping document nodes. */
enum class ParserDiagnosticCategory(
    val code: String,
) {
    MISSING_REQUIRED_MEMBER("parser.missing-required-member"),
    UNEXPECTED_VALUE_TYPE("parser.unexpected-value-type"),
}

/** JSON-compatible runtime value categories exposed by parser diagnostics. */
enum class ParserValueType(
    val displayName: String,
) {
    OBJECT("Map"),
    ARRAY("List"),
    STRING("String"),
    NUMBER("Number"),
    BOOLEAN("Boolean"),
    NULL("null"),
}

/** Structured failure produced by strict parser-node operations. */
sealed interface ParserDiagnostic {
    val category: ParserDiagnosticCategory
    val expectedType: String?
    val actualType: ParserValueType?
    val actualValue: Any?
    val path: String
    val sourceLocation: SourceLocation

    data class MissingRequiredMember(
        val memberName: String,
        override val path: String,
        override val sourceLocation: SourceLocation,
    ) : ParserDiagnostic {
        override val category: ParserDiagnosticCategory =
            ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER
        override val expectedType: String = "present member"
        override val actualType: ParserValueType? = null
        override val actualValue: Any? = null
    }

    data class UnexpectedValueType(
        override val expectedType: String,
        override val actualType: ParserValueType,
        override val actualValue: Any?,
        override val path: String,
        override val sourceLocation: SourceLocation,
    ) : ParserDiagnostic {
        override val category: ParserDiagnosticCategory =
            ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE
    }
}
