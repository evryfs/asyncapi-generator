package dev.banking.asyncapi.generator.core.validator.messages

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class MessageValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `message headers ref to component schema passes validation`() {
        val document = parse("src/test/resources/validator/messages/asyncapi_validator_message_headers_ref_valid.yaml")
        val results = asyncApiValidator.validate(document)
        assertFalse(results.hasErrors(), "Expected no errors for valid message headers ref.")
        assertFalse(results.hasWarnings(), "Expected no warnings for valid message headers ref.")
    }

    @Test
    fun `message headers broken ref triggers validation error`() {
        val document = parse("src/test/resources/validator/messages/asyncapi_validator_message_headers_ref_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertEquals(1, exception.errors.size, "Expected 1 error for unresolved message headers ref.")
    }
}
