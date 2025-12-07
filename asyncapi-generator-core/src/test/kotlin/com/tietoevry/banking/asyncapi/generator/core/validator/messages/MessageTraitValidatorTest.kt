package com.tietoevry.banking.asyncapi.generator.core.validator.messages

import com.tietoevry.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import com.tietoevry.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import com.tietoevry.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
}
