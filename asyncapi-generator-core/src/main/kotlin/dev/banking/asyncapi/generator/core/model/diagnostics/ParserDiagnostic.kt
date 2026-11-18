package dev.banking.asyncapi.generator.core.model.diagnostics

import dev.banking.asyncapi.generator.core.source.SourceLocation

/** Stable categories for failures produced while mapping document nodes. */
enum class ParserDiagnosticCategory(
    val code: String,
) {
    MISSING_REQUIRED_MEMBER("parser.missing-required-member"),
    UNEXPECTED_OBJECT_MEMBER("parser.unexpected-object-member"),
    UNEXPECTED_VALUE_TYPE("parser.unexpected-value-type"),
    INVALID_SPECIFICATION_VERSION("parser.invalid-specification-version"),
    UNSUPPORTED_SPECIFICATION_VERSION("parser.unsupported-specification-version"),
    INVALID_REFERENCE("parser.invalid-reference"),
    REFERENCE_DOCUMENT_NOT_FOUND("parser.reference-document-not-found"),
    REFERENCE_TARGET_NOT_FOUND("parser.reference-target-not-found"),
    LOAD_RESOURCE_LIMIT_EXCEEDED("parser.load-resource-limit-exceeded"),
    UNKNOWN_SCHEMA_FORMAT("parser.unknown-schema-format"),
    NATIVE_SCHEMA_ASSET_UNREADABLE("parser.native-schema-asset-unreadable"),
}

/** Load-scoped resource controlled by the parser loading boundary. */
enum class ParserLoadResourceLimit(
    val displayName: String,
) {
    SOURCE_DOCUMENTS("source documents"),
    REFERENCE_TARGETS("resolved reference targets"),
    EXTERNAL_REFERENCE_DEPTH("external reference depth"),
    AGGREGATE_SOURCE_BYTES("aggregate source bytes"),
    NATIVE_SCHEMA_ASSET_BYTES("native schema asset bytes"),
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

/** A structured parse or load failure: what failed and where. */
sealed interface ParserDiagnostic {
    /** The stable kind of failure. */
    val category: ParserDiagnosticCategory

    /** The display path of the node the failure is about. */
    val path: String

    /** Where in the source the failure is reported. */
    val sourceLocation: SourceLocation

    /**
     * An object lacks a member it must have.
     *
     * @property memberName the missing member
     * @property path where the member is missing
     * @property sourceLocation the object that lacks the member
     */
    data class MissingRequiredMember(
        val memberName: String,
        override val path: String,
        override val sourceLocation: SourceLocation,
    ) : ParserDiagnostic {
        override val category: ParserDiagnosticCategory =
            ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER
    }

    /**
     * A value has another type than its member requires.
     *
     * @property expectedType the required type
     * @property actualType the type found
     * @property actualValue the value found
     * @property path where the value is
     * @property sourceLocation where the value is written
     */
    data class UnexpectedValueType(
        val expectedType: String,
        val actualType: ParserValueType,
        val actualValue: Any?,
        override val path: String,
        override val sourceLocation: SourceLocation,
    ) : ParserDiagnostic {
        override val category: ParserDiagnosticCategory =
            ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE
    }

    /**
     * An object has a member its kind does not allow.
     *
     * @property memberName the unexpected member
     * @property objectType the kind of object, as named in the specification
     * @property specificationExtensionsAllowed whether the object allows `x-` members
     * @property path where the member is
     * @property sourceLocation where the member key is written
     */
    data class UnexpectedObjectMember(
        val memberName: String,
        val objectType: String,
        val specificationExtensionsAllowed: Boolean,
        override val path: String,
        override val sourceLocation: SourceLocation,
    ) : ParserDiagnostic {
        override val category: ParserDiagnosticCategory =
            ParserDiagnosticCategory.UNEXPECTED_OBJECT_MEMBER
        val expectedMember: String =
            if (specificationExtensionsAllowed) {
                "$objectType member or x- specification extension"
            } else {
                "$objectType member"
            }
    }

    /**
     * The `asyncapi` member is not a version number.
     *
     * @property declaredVersion the declared value
     * @property path where the version is
     * @property sourceLocation where the version is written
     */
    data class InvalidSpecificationVersion(
        val declaredVersion: String,
        override val path: String,
        override val sourceLocation: SourceLocation,
    ) : ParserDiagnostic {
        override val category: ParserDiagnosticCategory =
            ParserDiagnosticCategory.INVALID_SPECIFICATION_VERSION
    }

    /**
     * The document declares an AsyncAPI version this generator does not parse.
     *
     * @property declaredVersion the declared version
     * @property knownVersionLine whether the version belongs to a known version line without a parser
     * @property supportedVersionLines the version lines that are supported
     * @property path where the version is
     * @property sourceLocation where the version is written
     */
    data class UnsupportedSpecificationVersion(
        val declaredVersion: String,
        val knownVersionLine: Boolean,
        val supportedVersionLines: List<String>,
        override val path: String,
        override val sourceLocation: SourceLocation,
    ) : ParserDiagnostic {
        override val category: ParserDiagnosticCategory =
            ParserDiagnosticCategory.UNSUPPORTED_SPECIFICATION_VERSION
    }

    /**
     * A `$ref` is not a valid reference.
     *
     * @property reference the `$ref` value
     * @property reason why it is invalid
     * @property path where the reference is
     * @property sourceLocation where the reference is written
     */
    data class InvalidReference(
        val reference: String,
        val reason: String,
        override val path: String,
        override val sourceLocation: SourceLocation,
    ) : ParserDiagnostic {
        override val category: ParserDiagnosticCategory = ParserDiagnosticCategory.INVALID_REFERENCE
    }

    /**
     * The document a `$ref` points into cannot be read.
     *
     * @property reference the `$ref` value
     * @property resolvedFile the file the reference resolves to
     * @property path where the reference is
     * @property sourceLocation where the reference is written
     */
    data class ReferenceDocumentNotFound(
        val reference: String,
        val resolvedFile: String,
        override val path: String,
        override val sourceLocation: SourceLocation,
    ) : ParserDiagnostic {
        override val category: ParserDiagnosticCategory =
            ParserDiagnosticCategory.REFERENCE_DOCUMENT_NOT_FOUND
    }

    /**
     * The pointer of a `$ref` points at nothing in its document.
     *
     * @property reference the `$ref` value
     * @property path where the reference is
     * @property sourceLocation where the reference is written
     */
    data class ReferenceTargetNotFound(
        val reference: String,
        override val path: String,
        override val sourceLocation: SourceLocation,
    ) : ParserDiagnostic {
        override val category: ParserDiagnosticCategory =
            ParserDiagnosticCategory.REFERENCE_TARGET_NOT_FOUND
    }

    /**
     * The load exceeded one of its resource limits.
     *
     * @property limit the exceeded limit
     * @property maximum the limit's value
     * @property observed the value reached
     * @property path where the limit was exceeded
     * @property sourceLocation the input that exceeded it
     */
    data class LoadResourceLimitExceeded(
        val limit: ParserLoadResourceLimit,
        val maximum: Long,
        val observed: Long,
        override val path: String,
        override val sourceLocation: SourceLocation,
    ) : ParserDiagnostic {
        override val category: ParserDiagnosticCategory =
            ParserDiagnosticCategory.LOAD_RESOURCE_LIMIT_EXCEEDED
    }

    /**
     * A Multi Format Schema declares a `schemaFormat` this generator does not know.
     *
     * @property schemaFormat the declared format
     * @property path where the schema is
     * @property sourceLocation where the schema is declared
     */
    data class UnknownSchemaFormat(
        val schemaFormat: String,
        override val path: String,
        override val sourceLocation: SourceLocation,
    ) : ParserDiagnostic {
        override val category: ParserDiagnosticCategory = ParserDiagnosticCategory.UNKNOWN_SCHEMA_FORMAT
    }

    /**
     * The file a native Avro or Protobuf schema references cannot be read.
     *
     * @property reference the `$ref` value
     * @property reason why the file cannot be read
     * @property path where the reference is
     * @property sourceLocation where the reference is declared
     */
    data class NativeSchemaAssetUnreadable(
        val reference: String,
        val reason: String,
        override val path: String,
        override val sourceLocation: SourceLocation,
    ) : ParserDiagnostic {
        override val category: ParserDiagnosticCategory = ParserDiagnosticCategory.NATIVE_SCHEMA_ASSET_UNREADABLE
    }
}
