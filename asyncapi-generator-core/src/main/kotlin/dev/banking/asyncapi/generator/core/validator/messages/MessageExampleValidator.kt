package dev.banking.asyncapi.generator.core.validator.messages

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.messages.MessageExample
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_EXAMPLE_CONTENT_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_EXAMPLE_FORMAT_UNVALIDATED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_EXAMPLE_SCHEMA_MISMATCH
import dev.banking.asyncapi.generator.core.validator.schemas.SchemaInstanceValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

/** Validates rules owned by the AsyncAPI Message Example Object. */
internal class MessageExampleValidator(
    private val asyncApiContext: AsyncApiContext,
) {
    private val schemaInstanceValidator = SchemaInstanceValidator(asyncApiContext)

    fun validate(
        examples: List<MessageExample>,
        headersSchema: SchemaInterface?,
        payloadSchema: SchemaInterface?,
        contextString: String,
        results: ValidationCollector,
    ) {
        examples.forEachIndexed { index, example ->
            if (!results.visit(example)) return@forEachIndexed

            val sourceFields = asyncApiContext.getFieldNames(example)
            val containsHeaders = example.headers != null || "headers" in sourceFields
            val containsPayload = example.payload != null || "payload" in sourceFields
            if (!containsHeaders && !containsPayload) {
                results.error(
                    MESSAGE_EXAMPLE_CONTENT_REQUIRED,
                    "$contextString Example[$index] must contain 'headers', 'payload', or both.",
                    sourceLocation = asyncApiContext.getSourceLocation(example),
                )
            }

            if (containsHeaders && headersSchema != null) {
                validateValue(example, "headers", example.headers, headersSchema, contextString, index, results)
            }
            if (containsPayload && payloadSchema != null) {
                validateValue(example, "payload", example.payload, payloadSchema, contextString, index, results)
            }
        }
    }

    private fun validateValue(
        example: MessageExample,
        field: String,
        value: Any?,
        schema: SchemaInterface,
        contextString: String,
        index: Int,
        results: ValidationCollector,
    ) {
        val fieldLocation = asyncApiContext.getSourceLocation(example, field)
            ?: asyncApiContext.getSourceLocation(example)
        val basePath = fieldLocation?.path ?: field
        val evaluation = schemaInstanceValidator.validate(schema, value, basePath)
        evaluation.violations.distinct().forEach { violation ->
            results.error(
                MESSAGE_EXAMPLE_SCHEMA_MISMATCH,
                "$contextString Example[$index] '$field' ${violation.message}.",
                sourceLocation = sourceLocation(violation.path, basePath),
            )
        }
        evaluation.unsupportedFormats.distinct().forEach { unsupported ->
            results.warn(
                MESSAGE_EXAMPLE_FORMAT_UNVALIDATED,
                "$contextString Example[$index] '$field' cannot be validated because schema format " +
                    "'${unsupported.schemaFormat}' has no proven instance validator.",
                sourceLocation = sourceLocation(unsupported.path, basePath),
            )
        }
    }

    private fun sourceLocation(path: String, fallbackPath: String) =
        asyncApiContext.sourceRepository.getLocation(path)
            ?: asyncApiContext.sourceRepository.findNearestLocation(path)
            ?: asyncApiContext.sourceRepository.findNearestLocation(fallbackPath)
}
