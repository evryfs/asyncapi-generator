package dev.banking.asyncapi.generator.core.validator.parameters

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ParameterValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid parameters trigger errors and warnings`() {
        val document = parse("src/test/resources/validator/parameters/asyncapi_validator_parameter_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        val errorException = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        val errors = errorException.errors.map { it.message }
        assertEquals(2, errors.size, "Expected 2 validation errors.")

        assertTrue(results.hasWarnings(), "Expected warnings for non-critical issues.")
        val warnings = results.warnings.map { it.message }
        assertEquals(4, warnings.size, "Expected 4 validation warnings.")
    }
}
