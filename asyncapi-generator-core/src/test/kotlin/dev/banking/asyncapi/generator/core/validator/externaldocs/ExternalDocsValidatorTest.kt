package dev.banking.asyncapi.generator.core.validator.externaldocs

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExternalDocsValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid external docs trigger errors and warnings`() {
        val document = parse("src/test/resources/validator/externaldocs/asyncapi_validator_externaldocs_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertEquals(1, exception.errors.size, "Expected 1 error (invalid URL).")
    }
}
