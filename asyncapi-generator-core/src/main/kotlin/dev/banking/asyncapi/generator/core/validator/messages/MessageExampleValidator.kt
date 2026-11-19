package dev.banking.asyncapi.generator.core.validator.messages

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.messages.MessageExample
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_EXAMPLE_CONTENT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_EXAMPLE_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_EXAMPLE_SCHEMA
import dev.banking.asyncapi.generator.core.validator.schemas.SchemaInstanceValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath

/**
 * Validates rules owned by the AsyncAPI Message Example Object.
 *
 * @param asyncApiContext the load the examples belong to
 */
internal class MessageExampleValidator(
    private val asyncApiContext: AsyncApiContext,
) {
    private val schemaInstanceValidator = SchemaInstanceValidator(asyncApiContext)

    fun validate(
        examples: List<MessageExample>,
        headersSchema: SchemaInterface?,
        payloadSchema: SchemaInterface?,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        examples.forEachIndexed { index, example ->
            if (!results.visit(example)) return@forEachIndexed

            val site = ExampleSite(example, path, index)
            val sourceFields = asyncApiContext.modelTracking.getFieldNames(example)
            val containsHeaders = example.headers != null || "headers" in sourceFields
            val containsPayload = example.payload != null || "payload" in sourceFields
            if (!containsHeaders && !containsPayload) {
                results.error(
                    MESSAGE_EXAMPLE_CONTENT,
                    "$path Example[$index] must contain 'headers', 'payload', or both.",
                    sourceLocation = asyncApiContext.modelTracking.getSourceLocation(example),
                )
            }

            if (containsHeaders && headersSchema != null) {
                validateValue(site, "headers", example.headers, headersSchema, results)
            }
            if (containsPayload && payloadSchema != null) {
                validateValue(site, "payload", example.payload, payloadSchema, results)
            }
        }
    }

    private fun validateValue(
        site: ExampleSite,
        field: String,
        value: Any?,
        schema: SchemaInterface,
        results: ValidationCollector,
    ) {
        val (example, path, index) = site
        val fieldLocation =
            asyncApiContext.modelTracking.getSourceLocation(example, field)
                ?: asyncApiContext.modelTracking.getSourceLocation(example)
        val basePath = fieldLocation?.path ?: field
        val evaluation = schemaInstanceValidator.validate(schema, value, basePath)
        evaluation.violations.distinct().forEach { violation ->
            results.error(
                MESSAGE_EXAMPLE_SCHEMA,
                "$path Example[$index] '$field' ${violation.message}.",
                sourceLocation = sourceLocation(violation.path, basePath),
            )
        }
        evaluation.unsupportedFormats.distinct().forEach { unsupported ->
            results.warn(
                MESSAGE_EXAMPLE_FORMAT,
                "$path Example[$index] '$field' cannot be validated because schema format " +
                    "'${unsupported.schemaFormat}' has no proven instance validator.",
                sourceLocation = sourceLocation(unsupported.path, basePath),
            )
        }
    }

    private fun sourceLocation(
        path: String,
        fallbackPath: String,
    ) = asyncApiContext.nodeLocations.location(path)
        ?: asyncApiContext.nodeLocations.location(fallbackPath)

    /**
     * A message example and where it sits.
     *
     * @property example the example being checked
     * @property path the message the example belongs to, as findings name it
     * @property index the position of the example in the message's examples
     */
    private data class ExampleSite(
        val example: MessageExample,
        val path: ValidationPath,
        val index: Int,
    )
}
