package dev.banking.asyncapi.generator.core.validator.messages

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_CONTENT_TYPE_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_EXAMPLE_CONTENT_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_TRAIT_EMPTY
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
        assertEquals(2, exception.errors.size, "Expected content-type and example errors.")
        assertTrue(results.hasWarnings(), "Should have warnings.")

        assertRule(
            results,
            MESSAGE_CONTENT_TYPE_FORMAT,
            sourceFile = "asyncapi_validator_messagetrait_invalid.yaml",
            path = "asyncapi_validator_messagetrait_invalid.root.components.messageTraits.InvalidContentType.contentType",
            line = 10,
        )
        assertRule(
            results,
            MESSAGE_TRAIT_EMPTY,
            sourceFile = "asyncapi_validator_messagetrait_invalid.yaml",
            path = "asyncapi_validator_messagetrait_invalid.root.components.messageTraits.EmptyTrait",
            line = 15,
        )
        assertRule(
            results,
            MESSAGE_EXAMPLE_CONTENT_REQUIRED,
            sourceFile = "asyncapi_validator_messagetrait_invalid.yaml",
            path = "asyncapi_validator_messagetrait_invalid.root.components.messageTraits.InvalidExample.examples[0]",
            line = 22,
        )
    }

    @Test
    fun `accepts valid message traits including documentation-only traits and explicit null examples`() {
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
