package com.tietoevry.banking.asyncapi.generator.core.validator.servers

import com.tietoevry.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import com.tietoevry.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import com.tietoevry.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ServerValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid server definitions trigger errors and warnings`() {
        val document = parse("src/test/resources/validator/servers/asyncapi_validator_server_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertEquals(3, exception.errors.size, "Expected 3 validation errors.")

        assertTrue(results.hasWarnings(), "Should have warnings.")
        val warnings = results.warnings
        assertEquals(1, warnings.size, "Expected 1 validation warning.")
    }

    @Test
    fun `server variable mismatches trigger errors and warnings`() {
        val document = parse("src/test/resources/validator/servers/asyncapi_validator_server_variable_mismatch.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertEquals(1, exception.errors.size, "Expected 1 error for missing variable definition.")

        assertTrue(results.hasWarnings(), "Should have warnings.")
        val warnings = results.warnings
        assertEquals(1, warnings.size, "Expected 1 warning for missing variable definition.")
    }
}
