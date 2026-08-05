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
        val invalidFormat = results.findings.single { finding -> finding.code == CORRELATION_LOCATION_FORMAT.code }
        assertEquals(CORRELATION_LOCATION_FORMAT.severity, invalidFormat.severity)
        assertEquals(CORRELATION_LOCATION_FORMAT.concern, invalidFormat.concern)
        assertEquals("asyncapi_validator_correlation_invalid.yaml", invalidFormat.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_correlation_invalid.root.components.correlationIds.InvalidLocationRegex.location",
            invalidFormat.path,
        )
        assertEquals(10, invalidFormat.line)

        val missingLocation = results.findings.single { finding -> finding.code == CORRELATION_LOCATION_REQUIRED.code }
        assertEquals(CORRELATION_LOCATION_REQUIRED.severity, missingLocation.severity)
        assertEquals(CORRELATION_LOCATION_REQUIRED.concern, missingLocation.concern)
        assertEquals("asyncapi_validator_correlation_invalid.yaml", missingLocation.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_correlation_invalid.root.components.correlationIds.MissingLocation.location",
            missingLocation.path,
        )
        assertEquals(14, missingLocation.line)
    }

    @Test
    fun `valid correlation ID passes validation`() {
        val document = parse("validator/correlations/asyncapi_validator_correlation_valid.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(emptyList(), results.findings)
    }
}
