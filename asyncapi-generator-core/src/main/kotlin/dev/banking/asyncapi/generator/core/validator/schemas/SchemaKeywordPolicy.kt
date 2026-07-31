package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING

internal enum class SchemaKeywordCategory {
    SUPPORTED,
    RECOGNIZED_UNSUPPORTED,
    IGNORED_ANNOTATION,
    PERMITTED_EXTENSION,
    UNKNOWN,
}

internal data class SchemaKeywordClassification(
    val category: SchemaKeywordCategory,
    val severity: ValidationSeverity? = null,
    val explanation: String? = null,
    val doc: String? = null,
)

internal object SchemaKeywordPolicy {
    private const val ASYNCAPI_SCHEMA_OBJECT_DOC =
        "https://www.asyncapi.com/docs/reference/specification/v3.0.0#schemaObject"

    private val supportedKeywords =
        setOf(
            $$"$id",
            $$"$schema",
            $$"$comment",
            $$"$ref",
            "title",
            "description",
            "type",
            "format",
            "default",
            "examples",
            "multipleOf",
            "maximum",
            "exclusiveMaximum",
            "minimum",
            "exclusiveMinimum",
            "maxLength",
            "minLength",
            "pattern",
            "contentEncoding",
            "contentMediaType",
            "items",
            "additionalItems",
            "maxItems",
            "minItems",
            "uniqueItems",
            "contains",
            "maxProperties",
            "minProperties",
            "required",
            "properties",
            "patternProperties",
            "additionalProperties",
            "propertyNames",
            "dependencies",
            "definitions",
            "allOf",
            "anyOf",
            "oneOf",
            "not",
            "if",
            "then",
            "else",
            "enum",
            "const",
            "readOnly",
            "writeOnly",
            "discriminator",
            "deprecated",
            "externalDocs",
            "bindings",
        )

    private val unsupportedKeywords =
        mapOf(
            "nullable" to
                "is not supported by the AsyncAPI 3.0 Schema Object semantics used by the generator. Express " +
                    """nullability with a type array, for example type: ["string", "null"].""",
            "xml" to
                "is not supported by the AsyncAPI 3.0 Schema Object semantics used by the generator. Remove it " +
                    "or represent transport-specific behavior outside the payload Schema Object.",
            $$"$defs" to
                "is not supported under the generator's JSON Schema Draft 7 semantics. Use the 'definitions' " +
                    "keyword instead.",
            "dependentRequired" to
                "is not supported under the generator's JSON Schema Draft 7 semantics. Use the 'dependencies' " +
                    "keyword instead.",
            "dependentSchemas" to
                "is not supported under the generator's JSON Schema Draft 7 semantics. Use the 'dependencies' " +
                    "keyword instead.",
            "prefixItems" to
                "is not supported under the generator's JSON Schema Draft 7 semantics. Use a single Schema " +
                    "Object in 'items'.",
            "unevaluatedItems" to
                "is not supported under the generator's JSON Schema Draft 7 semantics.",
            "unevaluatedProperties" to
                "is not supported under the generator's JSON Schema Draft 7 semantics.",
            "minContains" to
                "is not supported under the generator's JSON Schema Draft 7 semantics.",
            "maxContains" to
                "is not supported under the generator's JSON Schema Draft 7 semantics.",
            $$"$anchor" to
                "is not supported under the generator's JSON Schema Draft 7 semantics.",
            $$"$dynamicAnchor" to
                "is not supported under the generator's JSON Schema Draft 7 semantics.",
            $$"$dynamicRef" to
                "is not supported under the generator's JSON Schema Draft 7 semantics.",
            "contentSchema" to
                "is not supported under the generator's JSON Schema Draft 7 semantics.",
        )

    fun classify(keyword: String): SchemaKeywordClassification =
        when {
            keyword in supportedKeywords ->
                SchemaKeywordClassification(SchemaKeywordCategory.SUPPORTED)

            keyword.startsWith("x-") ->
                SchemaKeywordClassification(SchemaKeywordCategory.PERMITTED_EXTENSION)

            keyword == "example" ->
                SchemaKeywordClassification(
                    category = SchemaKeywordCategory.IGNORED_ANNOTATION,
                    severity = WARNING,
                    explanation =
                        "is an unsupported annotation and will not be retained. Use the AsyncAPI-compatible " +
                            "'examples' array instead.",
                    doc = ASYNCAPI_SCHEMA_OBJECT_DOC,
                )

            keyword in unsupportedKeywords ->
                SchemaKeywordClassification(
                    category = SchemaKeywordCategory.RECOGNIZED_UNSUPPORTED,
                    severity = ERROR,
                    explanation = unsupportedKeywords.getValue(keyword),
                    doc = ASYNCAPI_SCHEMA_OBJECT_DOC,
                )

            else ->
                SchemaKeywordClassification(
                    category = SchemaKeywordCategory.UNKNOWN,
                    severity = ERROR,
                    explanation =
                        "is not supported by the generator and would otherwise be ignored. Use an AsyncAPI 3.0 " +
                            "Schema Object keyword or an 'x-' extension property.",
                    doc = ASYNCAPI_SCHEMA_OBJECT_DOC,
                )
        }
}
