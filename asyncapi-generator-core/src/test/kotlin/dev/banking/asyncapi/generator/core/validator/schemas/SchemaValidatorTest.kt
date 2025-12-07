package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `valid simple schema passes validation`() {
        val document = parse("src/test/resources/validator/schemas/asyncapi_validator_schema_valid_simple.yaml")
        val results = asyncApiValidator.validate(document)

        assertFalse(results.hasErrors(), "Expected no validation errors, found: ${results.errors}")
        assertFalse(results.hasWarnings(), "Expected no validation warnings, found: ${results.warnings}")
    }

    @Test
    fun `schema with invalid type field throws validation error`() {
        val document = parse("src/test/resources/validator/schemas/asyncapi_validator_schema_invalid_type.yaml")
        val validationResults = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            validationResults.throwErrors()
        }

        assertEquals(1, exception.errors.size, "Expected exactly one validation error for invalid schema type.")
    }

    @Test
    fun `schema with invalid numeric and string constraints throws validation errors`() {
        val document =
            parse("src/test/resources/validator/schemas/asyncapi_validator_schema_invalid_constraints.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertEquals(3, exception.errors.size, "Expected 3 validation errors for invalid constraints.")
    }

    @Test
    fun `schema with invalid discriminator definition throws validation errors`() {
        val document =
            parse("src/test/resources/validator/schemas/asyncapi_validator_schema_invalid_discriminator.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertEquals(2, exception.errors.size, "Expected 2 errors for invalid discriminator definitions.")
    }

    @Test
    fun `schema with incompatible default value throws validation errors`() {
        val document = parse("src/test/resources/validator/schemas/asyncapi_validator_schema_invalid_default.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertEquals(3, exception.errors.size, "Expected 3 validation errors for incompatible default values.")
    }

    @Test
    fun `schema with incompatible const value throws validation errors`() {
        val document = parse("src/test/resources/validator/schemas/asyncapi_validator_schema_invalid_const.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertEquals(3, exception.errors.size, "Expected 3 validation errors for incompatible const values.")
    }

    @Test
    fun `schema with ambiguous composition or empty fields triggers warnings`() {
        val document = parse("src/test/resources/validator/schemas/asyncapi_validator_schema_warnings.yaml")
        val results = asyncApiValidator.validate(document)

        assertFalse(results.hasErrors(), "Should not have errors for warnings.")
        assertTrue(results.hasWarnings(), "Should have warnings.")

        val warnings = results.warnings
        assertEquals(4, warnings.size, "Expected 3 warnings for incompatible composition values.")
    }

    @Test
    fun `schema with invalid array or object structure throws validation errors`() {
        val document = parse("src/test/resources/validator/schemas/asyncapi_validator_schema_invalid_structure.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertEquals(2, exception.errors.size)
    }
}
