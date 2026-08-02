package dev.banking.asyncapi.generator.core.validator.externaldocs

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExternalDocsValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid external docs trigger errors and warnings`() {
        val document = parse("validator/externaldocs/asyncapi_validator_externaldocs_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            throwErrors(results)
        }
        assertEquals(1, exception.errors.size, "Expected 1 error (invalid URL).")
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "'url' must be a valid absolute URL",
            sourceFile = "asyncapi_validator_externaldocs_invalid.yaml",
            path = "asyncapi_validator_externaldocs_invalid.root.components.schemas.InvalidExternalDoc.externalDocs.url",
            line = 10,
        )
    }
}
