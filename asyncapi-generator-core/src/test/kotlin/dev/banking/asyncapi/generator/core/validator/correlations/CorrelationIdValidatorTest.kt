package dev.banking.asyncapi.generator.core.validator.correlations

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CORRELATION_LOCATION_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CORRELATION_LOCATION_REQUIRED
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CorrelationIdValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid correlation ID runtime expression triggers an error`() {
        val document = parse("validator/correlations/asyncapi_validator_correlation_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        assertEquals(2, results.errors.size)
        assertRule(
            results,
            rule = CORRELATION_LOCATION_FORMAT,
            sourceFile = "asyncapi_validator_correlation_invalid.yaml",
            path = "asyncapi_validator_correlation_invalid.root.components.correlationIds.InvalidLocationRegex.location",
            line = 10,
        )
        assertRule(
            results,
            rule = CORRELATION_LOCATION_REQUIRED,
            sourceFile = "asyncapi_validator_correlation_invalid.yaml",
            path = "asyncapi_validator_correlation_invalid.root.components.correlationIds.MissingLocation.location",
            line = 14,
        )
    }

    @Test
    fun `valid correlation ID passes validation`() {
        val document = parse("validator/correlations/asyncapi_validator_correlation_valid.yaml")
        val results = asyncApiValidator.validate(document)
        assertNoFindings(results)
    }
}
