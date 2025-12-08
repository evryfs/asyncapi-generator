package dev.banking.asyncapi.generator.core.validator.security

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SecuritySchemeValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid security schemes trigger validation errors`() {
        val document = parse("src/test/resources/validator/security/asyncapi_validator_security_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> { results.throwErrors() }
        assertEquals(6, exception.errors.size, "Expected 6 validation errors.")
    }

    @Test
    fun `security schemes with missing optional fields trigger warnings`() {
        val document = parse("src/test/resources/validator/security/asyncapi_validator_security_warnings.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertEquals(1, exception.errors.size, "Expected 1 error (missing name).")
        assertTrue(results.hasWarnings(), "Should have warnings.")
    }
}
