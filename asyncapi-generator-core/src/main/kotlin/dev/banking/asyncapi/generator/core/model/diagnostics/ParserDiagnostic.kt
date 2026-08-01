package dev.banking.asyncapi.generator.core.model.diagnostics

import dev.banking.asyncapi.generator.core.reader.SourceLocation

/** Stable categories for failures produced while mapping document nodes. */
enum class ParserDiagnosticCategory(
    val code: String,
) {
    MISSING_REQUIRED_MEMBER("parser.missing-required-member"),
    UNEXPECTED_VALUE_TYPE("parser.unexpected-value-type"),
    INVALID_REFERENCE("parser.invalid-reference"),
    REFERENCE_DOCUMENT_NOT_FOUND("parser.reference-document-not-found"),
    REFERENCE_TARGET_NOT_FOUND("parser.reference-target-not-found"),
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

    data class InvalidReference(
        val reference: String,
        val reason: String,
        override val path: String,
        override val sourceLocation: SourceLocation,
    ) : ParserDiagnostic {
        override val category: ParserDiagnosticCategory = ParserDiagnosticCategory.INVALID_REFERENCE
        override val expectedType: String = "valid URI reference with a JSON Pointer fragment"
        override val actualType: ParserValueType = ParserValueType.STRING
        override val actualValue: Any = reference
    }

    data class ReferenceDocumentNotFound(
        val reference: String,
        val resolvedFile: String,
        override val path: String,
        override val sourceLocation: SourceLocation,
    ) : ParserDiagnostic {
        override val category: ParserDiagnosticCategory =
            ParserDiagnosticCategory.REFERENCE_DOCUMENT_NOT_FOUND
        override val expectedType: String = "existing readable reference document"
        override val actualType: ParserValueType = ParserValueType.STRING
        override val actualValue: Any = reference
    }

    data class ReferenceTargetNotFound(
        val reference: String,
        override val path: String,
        override val sourceLocation: SourceLocation,
    ) : ParserDiagnostic {
        override val category: ParserDiagnosticCategory =
            ParserDiagnosticCategory.REFERENCE_TARGET_NOT_FOUND
        override val expectedType: String = "existing JSON Pointer target"
        override val actualType: ParserValueType = ParserValueType.STRING
        override val actualValue: Any = reference
    }
}
