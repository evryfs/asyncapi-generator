package com.tietoevry.banking.asyncapi.generator.core.validator.externaldocs

import com.tietoevry.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import com.tietoevry.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import com.tietoevry.banking.asyncapi.generator.core.validator.AsyncApiValidator
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

        assertTrue(results.hasWarnings(), "Should have warnings.")
        val warnings = results.warnings.map { it.message }
        assertEquals(1, warnings.size, "Expected 1 warning (empty description).")
    }
}
