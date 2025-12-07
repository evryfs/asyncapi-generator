package com.tietoevry.banking.asyncapi.generator.core.validator.tags

import com.tietoevry.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import com.tietoevry.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import com.tietoevry.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TagValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid tags trigger errors and warnings`() {
        val document = parse("src/test/resources/validator/tags/asyncapi_validator_tag_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertEquals(1, exception.errors.size, "Expected 1 error (empty name).")

        assertTrue(results.hasWarnings(), "Should have warnings.")
        val warnings = results.warnings
        assertEquals(1, warnings.size, "Expected 1 warning (short description).")
    }
}
