package dev.banking.asyncapi.generator.core.validator.messages

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageTraitValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid message traits trigger errors and warnings`() {
        val document = parse("src/test/resources/validator/messages/asyncapi_validator_messagetrait_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertEquals(1, exception.errors.size, "Expected 1 error (content type).")
        assertTrue(results.hasWarnings(), "Should have warnings.")
    }

    @Test
    fun `message trait headers ref to component schema passes validation`() {
        val document = parse("src/test/resources/validator/messages/asyncapi_validator_messagetrait_headers_ref_valid.yaml")
        val results = asyncApiValidator.validate(document)
        assertFalse(results.hasErrors(), "Expected no errors for valid message trait headers ref.")
        assertFalse(results.hasWarnings(), "Expected no warnings for valid message trait headers ref.")
    }

    @Test
    fun `message trait headers broken ref triggers validation error`() {
        val document = parse("src/test/resources/validator/messages/asyncapi_validator_messagetrait_headers_ref_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertEquals(1, exception.errors.size, "Expected 1 error for unresolved message trait headers ref.")
    }
}
