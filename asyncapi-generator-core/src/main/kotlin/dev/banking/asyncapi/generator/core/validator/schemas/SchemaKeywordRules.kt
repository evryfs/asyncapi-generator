package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ANNOTATION_IGNORED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DIALECT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_KEYWORD_UNSUPPORTED
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath

/**
 * Validates which keywords a schema declares and which JSON Schema dialect it names.
 *
 * @param asyncApiContext the load the schemas belong to
 */
internal class SchemaKeywordRules(
    private val asyncApiContext: AsyncApiContext,
) {
    /** Reports every keyword of [node] that the generator ignores or does not support. */
    fun checkKeywords(
        node: Any,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        asyncApiContext.modelTracking.getFieldNames(node).forEach { keyword ->
            val classification = SchemaKeywordPolicy.classify(keyword)
            val severity = classification.severity ?: return@forEach
            val explanation = classification.explanation ?: return@forEach
            val rule: ValidationRule =
                if (classification.category == SchemaKeywordCategory.IGNORED_ANNOTATION) {
                    SCHEMA_ANNOTATION_IGNORED
                } else {
                    SCHEMA_KEYWORD_UNSUPPORTED
                }
            val message = "$path Schema Object keyword '$keyword' $explanation"
            val sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, keyword)
            when (severity) {
                ERROR -> results.error(rule = rule, message = message, sourceLocation = sourceLocation)
                WARNING -> results.warn(rule = rule, message = message, sourceLocation = sourceLocation)
            }
        }
    }

    /** Reports a `$schema` keyword that is empty or names a dialect other than JSON Schema Draft 7. */
    fun checkDialect(schema: CheckedSchema) {
        val node = schema.node
        if ("\$schema" !in asyncApiContext.modelTracking.getFieldNames(node)) return
        val dialect = node.schema
        val message =
            when {
                dialect == null -> missingDialectMessage(schema.path)
                dialect.removeSuffix("#") !in SUPPORTED_DRAFT_7_DIALECTS ->
                    unsupportedDialectMessage(schema.path, dialect)
                else -> return
            }
        schema.results.error(
            SCHEMA_DIALECT,
            message,
            sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, "\$schema"),
        )
    }

    private fun missingDialectMessage(path: ValidationPath): String =
        "$path Schema Object keyword '\$schema' must contain a schema dialect URI."

    private fun unsupportedDialectMessage(
        path: ValidationPath,
        dialect: String,
    ): String =
        "$path declares schema dialect '$dialect', but the generator supports AsyncAPI 3.0 Schema " +
            "Object semantics based on JSON Schema Draft 7. Remove '\$schema' or declare the Draft 7 dialect."

    private companion object {
        val SUPPORTED_DRAFT_7_DIALECTS =
            setOf(
                "http://json-schema.org/draft-07/schema",
                "https://json-schema.org/draft-07/schema",
            )
    }
}
