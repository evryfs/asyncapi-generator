package dev.banking.asyncapi.generator.core.validator.info

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InfoValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `valid info object passes validation`() {
        val asyncApiDocument = parse("src/test/resources/validator/info/asyncapi_validator_info_valid_simple.yaml")
        val validationResults = asyncApiValidator.validate(asyncApiDocument)

        assertFalse(validationResults.hasErrors(), "Found validation errors: ${validationResults.errors}")
        assertFalse(validationResults.hasWarnings(), "Found validation warnings: ${validationResults.warnings}")
    }

    @Test
    fun `validation reports multiple errors for invalid info object`() {
        val asyncApiDocument = parse("src/test/resources/validator/info/asyncapi_validator_info_multiple_errors.yaml")
        val validationResults = asyncApiValidator.validate(asyncApiDocument)

        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            validationResults.throwErrors()
        }
        assertEquals(2, exception.errors.size, "Expected exactly 2 validation errors (title and version).")
    }

    @Test
    fun `invalid contact and license info trigger errors and warnings`() {
        val document = parse("src/test/resources/validator/info/asyncapi_validator_info_components_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }

        assertEquals(4, exception.errors.size, "Expected 4 validation errors.")
    }
}
