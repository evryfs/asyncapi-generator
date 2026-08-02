package dev.banking.asyncapi.generator.core.validator.info

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CONTACT_EMAIL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CONTACT_EMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CONTACT_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.INFO_TERMS_OF_SERVICE_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.INFO_TITLE_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.INFO_VERSION_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.INFO_VERSION_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.LICENSE_NAME_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.LICENSE_URL_FORMAT
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
        assertRule(validationResults, INFO_TITLE_REQUIRED, path = "asyncapi_validator_info_multiple_errors.root.info.title", line = 3)
        assertRule(validationResults, INFO_VERSION_REQUIRED, path = "asyncapi_validator_info_multiple_errors.root.info.version", line = 4)
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

        assertRule(
            results,
            CONTACT_URL_FORMAT,
            sourceFile = "asyncapi_validator_info_components_invalid.yaml",
            path = "asyncapi_validator_info_components_invalid.root.info.contact.url",
            line = 7,
        )
        assertRule(
            results,
            CONTACT_EMAIL_FORMAT,
            sourceFile = "asyncapi_validator_info_components_invalid.yaml",
            path = "asyncapi_validator_info_components_invalid.root.info.contact.email",
            line = 8,
        )
        assertRule(
            results,
            LICENSE_NAME_REQUIRED,
            sourceFile = "asyncapi_validator_info_components_invalid.yaml",
            path = "asyncapi_validator_info_components_invalid.root.info.license.name",
            line = 10,
        )
        assertRule(
            results,
            LICENSE_URL_FORMAT,
            sourceFile = "asyncapi_validator_info_components_invalid.yaml",
            path = "asyncapi_validator_info_components_invalid.root.info.license.url",
            line = 11,
        )
    }

    @Test
    fun `info advisories and terms URI use distinct stable rules`() {
        val results = validate("validator/info/asyncapi_validator_info_diagnostics.yaml")

        assertEquals(1, results.errors.size)
        assertEquals(2, results.warnings.size)
        assertRule(results, INFO_VERSION_FORMAT, path = "asyncapi_validator_info_diagnostics.root.info.version", line = 4)
        assertRule(
            results,
            INFO_TERMS_OF_SERVICE_FORMAT,
            path = "asyncapi_validator_info_diagnostics.root.info.termsOfService",
            line = 5,
        )
        assertRule(results, CONTACT_EMPTY, path = "asyncapi_validator_info_diagnostics.root.info.contact", line = 6)
    }
}
