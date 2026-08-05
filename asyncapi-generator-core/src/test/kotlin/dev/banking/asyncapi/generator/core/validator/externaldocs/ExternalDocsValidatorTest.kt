package dev.banking.asyncapi.generator.core.validator.externaldocs

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.EXTERNAL_DOC_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.EXTERNAL_DOC_URL_REQUIRED
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ExternalDocsValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid external docs trigger errors only`() {
        val document = parse("validator/externaldocs/asyncapi_validator_externaldocs_invalid.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(2, results.errors.size)
        val invalid = results.findings.single { it.code == EXTERNAL_DOC_URL_FORMAT.code }
        assertEquals(EXTERNAL_DOC_URL_FORMAT.severity, invalid.severity)
        assertEquals(EXTERNAL_DOC_URL_FORMAT.concern, invalid.concern)
        assertEquals("asyncapi_validator_externaldocs_invalid.yaml", invalid.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_externaldocs_invalid.root.components.schemas.InvalidExternalDoc.externalDocs.url",
            invalid.path,
        )
        assertEquals(10, invalid.line)

        val required = results.findings.single { it.code == EXTERNAL_DOC_URL_REQUIRED.code }
        assertEquals(EXTERNAL_DOC_URL_REQUIRED.severity, required.severity)
        assertEquals(EXTERNAL_DOC_URL_REQUIRED.concern, required.concern)
        assertEquals("asyncapi_validator_externaldocs_invalid.yaml", required.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_externaldocs_invalid.root.components.schemas.MissingExternalDocUrl.externalDocs.url",
            required.path,
        )
        assertEquals(15, required.line)
    }
}
