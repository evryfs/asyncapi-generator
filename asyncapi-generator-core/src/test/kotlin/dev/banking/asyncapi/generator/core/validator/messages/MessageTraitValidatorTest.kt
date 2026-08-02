package dev.banking.asyncapi.generator.core.validator.messages

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
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
        val document = parse("validator/messages/asyncapi_validator_messagetrait_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            throwErrors(results)
        }
        assertEquals(1, exception.errors.size, "Expected 1 error (content type).")
        assertTrue(results.hasWarnings(), "Should have warnings.")

        assertFinding(
            results,
            severity = ERROR,
            messageContains = "invalid 'contentType' value",
            sourceFile = "asyncapi_validator_messagetrait_invalid.yaml",
            path = "asyncapi_validator_messagetrait_invalid.root.components.messageTraits.InvalidContentType.contentType",
            line = 10,
        )
        assertFinding(
            results,
            severity = WARNING,
            messageContains = "provides neither 'headers', 'bindings', 'correlationId', nor 'contentType'",
            sourceFile = "asyncapi_validator_messagetrait_invalid.yaml",
            path = "asyncapi_validator_messagetrait_invalid.root.components.messageTraits.MeaninglessTrait",
            line = 15,
        )
    }

    @Test
    fun `message trait headers ref to component schema passes validation`() {
        val document = parse("validator/messages/asyncapi_validator_messagetrait_headers_ref_valid.yaml")
        val results = asyncApiValidator.validate(document)
        assertNoFindings(results)
    }

    @Test
    fun `message trait headers broken ref triggers validation error`() {
        val document = parse("validator/messages/asyncapi_validator_messagetrait_headers_ref_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            throwErrors(results)
        }
        assertEquals(1, exception.errors.size, "Expected 1 error for unresolved message trait headers ref.")
    }
}
