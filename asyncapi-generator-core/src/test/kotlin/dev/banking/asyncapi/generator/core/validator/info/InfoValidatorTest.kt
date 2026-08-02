package dev.banking.asyncapi.generator.core.validator.info

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InfoValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `valid info object passes validation`() {
        val asyncApiDocument = parse("validator/info/asyncapi_validator_info_valid_simple.yaml")
        val validationResults = asyncApiValidator.validate(asyncApiDocument)

        assertNoFindings(validationResults)
    }

    @Test
    fun `validation reports multiple errors for invalid info object`() {
        val asyncApiDocument = parse("validator/info/asyncapi_validator_info_multiple_errors.yaml")
        val validationResults = asyncApiValidator.validate(asyncApiDocument)

        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            throwErrors(validationResults)
        }
        assertEquals(2, exception.errors.size, "Expected exactly 2 validation errors (title and version).")
    }

    @Test
    fun `invalid contact and license info trigger errors and warnings`() {
        val document = parse("validator/info/asyncapi_validator_info_components_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            throwErrors(results)
        }

        assertEquals(4, exception.errors.size, "Expected 4 validation errors.")
        assertEquals(4, results.findings.size)

        assertFinding(
            results,
            severity = ERROR,
            messageContains = "'url' field must be a valid absolute URL",
            sourceFile = "asyncapi_validator_info_components_invalid.yaml",
            path = "asyncapi_validator_info_components_invalid.root.info.contact.url",
            line = 7,
        )
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "'email' field must be a valid email address",
            sourceFile = "asyncapi_validator_info_components_invalid.yaml",
            path = "asyncapi_validator_info_components_invalid.root.info.contact.email",
            line = 8,
        )
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "'name' field is required and cannot be empty",
            sourceFile = "asyncapi_validator_info_components_invalid.yaml",
            path = "asyncapi_validator_info_components_invalid.root.info.license.name",
            line = 10,
        )
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "'url' field must be a valid absolute URL",
            sourceFile = "asyncapi_validator_info_components_invalid.yaml",
            path = "asyncapi_validator_info_components_invalid.root.info.license.url",
            line = 11,
        )
    }
}
