package dev.banking.asyncapi.generator.core.validator.messages

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.messages.MessageExample
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_EXAMPLE_CONTENT_REQUIRED
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

/** Validates rules owned by the AsyncAPI Message Example Object. */
internal class MessageExampleValidator(
    private val asyncApiContext: AsyncApiContext,
) {

    fun validate(examples: List<MessageExample>, contextString: String, results: ValidationCollector) {
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
        }
    }
}
