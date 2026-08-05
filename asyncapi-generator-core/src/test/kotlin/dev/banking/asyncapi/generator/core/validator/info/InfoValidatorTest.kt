package dev.banking.asyncapi.generator.core.validator.info

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.INFO_TERMS_OF_SERVICE_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.INFO_TITLE_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.INFO_VERSION_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CONTACT_EMAIL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CONTACT_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.LICENSE_NAME_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.LICENSE_URL_FORMAT
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class InfoValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `valid info object passes validation`() {
        val asyncApiDocument = parse("validator/info/asyncapi_validator_info_valid_simple.yaml")
        val validationResults = asyncApiValidator.validate(asyncApiDocument)

        assertEquals(emptyList(), validationResults.findings)
    }

    @Test
    fun `validation reports multiple errors for invalid info object`() {
        val asyncApiDocument = parse("validator/info/asyncapi_validator_info_multiple_errors.yaml")
        val validationResults = asyncApiValidator.validate(asyncApiDocument)

        assertEquals(2, validationResults.errors.size, "Expected exactly 2 validation errors (title and version).")
        val title = validationResults.findings.single { it.code == INFO_TITLE_REQUIRED.code }
        assertEquals(INFO_TITLE_REQUIRED.severity, title.severity)
        assertEquals(INFO_TITLE_REQUIRED.concern, title.concern)
        assertEquals("asyncapi_validator_info_multiple_errors.yaml", title.sourceLocation?.file?.name)
        assertEquals("asyncapi_validator_info_multiple_errors.root.info.title", title.path)
        assertEquals(3, title.line)

        val version = validationResults.findings.single { it.code == INFO_VERSION_REQUIRED.code }
        assertEquals(INFO_VERSION_REQUIRED.severity, version.severity)
        assertEquals(INFO_VERSION_REQUIRED.concern, version.concern)
        assertEquals("asyncapi_validator_info_multiple_errors.yaml", version.sourceLocation?.file?.name)
        assertEquals("asyncapi_validator_info_multiple_errors.root.info.version", version.path)
        assertEquals(4, version.line)
    }

    @Test
    fun `invalid contact and license info trigger errors only`() {
        val document = parse("validator/info/asyncapi_validator_info_components_invalid.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(4, results.errors.size)
        assertEquals(4, results.findings.size)

        val contactUrl = results.findings.single { it.code == CONTACT_URL_FORMAT.code }
        assertEquals(CONTACT_URL_FORMAT.severity, contactUrl.severity)
        assertEquals(CONTACT_URL_FORMAT.concern, contactUrl.concern)
        assertEquals("asyncapi_validator_info_components_invalid.yaml", contactUrl.sourceLocation?.file?.name)
        assertEquals("asyncapi_validator_info_components_invalid.root.info.contact.url", contactUrl.path)
        assertEquals(7, contactUrl.line)

        val contactEmail = results.findings.single { it.code == CONTACT_EMAIL_FORMAT.code }
        assertEquals(CONTACT_EMAIL_FORMAT.severity, contactEmail.severity)
        assertEquals(CONTACT_EMAIL_FORMAT.concern, contactEmail.concern)
        assertEquals("asyncapi_validator_info_components_invalid.yaml", contactEmail.sourceLocation?.file?.name)
        assertEquals("asyncapi_validator_info_components_invalid.root.info.contact.email", contactEmail.path)
        assertEquals(8, contactEmail.line)

        val licenseName = results.findings.single { it.code == LICENSE_NAME_REQUIRED.code }
        assertEquals(LICENSE_NAME_REQUIRED.severity, licenseName.severity)
        assertEquals(LICENSE_NAME_REQUIRED.concern, licenseName.concern)
        assertEquals("asyncapi_validator_info_components_invalid.yaml", licenseName.sourceLocation?.file?.name)
        assertEquals("asyncapi_validator_info_components_invalid.root.info.license.name", licenseName.path)
        assertEquals(10, licenseName.line)

        val licenseUrl = results.findings.single { it.code == LICENSE_URL_FORMAT.code }
        assertEquals(LICENSE_URL_FORMAT.severity, licenseUrl.severity)
        assertEquals(LICENSE_URL_FORMAT.concern, licenseUrl.concern)
        assertEquals("asyncapi_validator_info_components_invalid.yaml", licenseUrl.sourceLocation?.file?.name)
        assertEquals("asyncapi_validator_info_components_invalid.root.info.license.url", licenseUrl.path)
        assertEquals(11, licenseUrl.line)
    }

    @Test
    fun `invalid terms of service URI produces a specification error`() {
        val results = validate("validator/info/asyncapi_validator_info_diagnostics.yaml")

        assertEquals(1, results.findings.size)

        val termsUri = results.findings.single { it.code == INFO_TERMS_OF_SERVICE_FORMAT.code }
        assertEquals(INFO_TERMS_OF_SERVICE_FORMAT.severity, termsUri.severity)
        assertEquals(INFO_TERMS_OF_SERVICE_FORMAT.concern, termsUri.concern)
        assertEquals(
            "asyncapi_validator_info_diagnostics.yaml",
            termsUri.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_info_diagnostics.root.info.termsOfService",
            termsUri.path,
        )
        assertEquals(5, termsUri.line)
    }
}
