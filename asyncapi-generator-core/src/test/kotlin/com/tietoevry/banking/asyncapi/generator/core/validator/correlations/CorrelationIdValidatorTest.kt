package com.tietoevry.banking.asyncapi.generator.core.validator.correlations

import com.tietoevry.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import com.tietoevry.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CorrelationIdValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid correlation IDs trigger warnings`() {
        val document = parse("src/test/resources/validator/correlations/asyncapi_validator_correlation_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        assertFalse(results.hasErrors(), "Expected no errors, but found: ${results.errors}")

        val warnings = results.warnings
        assertEquals(2, warnings.size, "Expected 2 warnings.")
    }

    @Test
    fun `valid correlation ID passes validation`() {
        val document = parse("src/test/resources/validator/correlations/asyncapi_validator_correlation_valid.yaml")
        val results = asyncApiValidator.validate(document)
        assertFalse(results.hasErrors(), "Expected no errors for valid correlation ID.")
        assertFalse(results.hasWarnings(), "Expected no warnings for valid correlation ID.")
    }
}
