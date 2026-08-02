package dev.banking.asyncapi.generator.core.validator.messages

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_CONTENT_TYPE_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_EXAMPLE_CONTENT_REQUIRED
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class MessageValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `validates message content type and example structure`() {
        val results = validate("validator/messages/asyncapi_validator_message_invalid.yaml")

        assertEquals(2, results.errors.size)
        assertRule(
            results,
            MESSAGE_CONTENT_TYPE_FORMAT,
            sourceFile = "asyncapi_validator_message_invalid.yaml",
            path = "asyncapi_validator_message_invalid.root.components.messages.InvalidContentType.contentType",
            line = 8,
        )
        assertRule(
            results,
            MESSAGE_EXAMPLE_CONTENT_REQUIRED,
            sourceFile = "asyncapi_validator_message_invalid.yaml",
            path = "asyncapi_validator_message_invalid.root.components.messages.InvalidExample.examples[0]",
            line = 11,
        )
    }

    @Test
    fun `accepts specific content types and examples containing headers or an explicit null payload`() {
        val results = validate("validator/messages/asyncapi_validator_message_valid.yaml")

        assertNoFindings(results)
    }

    @Test
    fun `message headers ref to component schema passes validation`() {
        val document = parse("validator/messages/asyncapi_validator_message_headers_ref_valid.yaml")
        val results = asyncApiValidator.validate(document)
        assertFalse(results.hasErrors(), "Expected no errors for valid message headers ref.")
        assertFalse(results.hasWarnings(), "Expected no warnings for valid message headers ref.")
    }

    @Test
    fun `message headers broken ref triggers validation error`() {
        val document = parse("validator/messages/asyncapi_validator_message_headers_ref_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            throwErrors(results)
        }
        assertEquals(1, exception.errors.size, "Expected 1 error for unresolved message headers ref.")
    }
}
