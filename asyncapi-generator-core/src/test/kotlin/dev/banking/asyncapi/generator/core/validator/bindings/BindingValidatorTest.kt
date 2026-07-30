package dev.banking.asyncapi.generator.core.validator.bindings

import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BindingValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid bindings trigger warnings`() {
        val document = parse("validator/bindings/asyncapi_validator_binding_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        assertFalse(results.hasErrors(), "Expected no errors, but found: ${results.errors}")

        val warnings = results.warnings.map { it.message }
        assertEquals(3, warnings.size, "Expected 3 warnings for invalid bindings.")
        assertFinding(
            results,
            severity = WARNING,
            messageContains = "is empty",
            sourceFile = "asyncapi_validator_binding_invalid.yaml",
            path = "asyncapi_validator_binding_invalid.root.components.channelBindings.EmptyBinding",
            line = 8,
        )
        assertFinding(
            results,
            severity = WARNING,
            messageContains = "Property 'protocol'",
            sourceFile = "asyncapi_validator_binding_invalid.yaml",
            path = "asyncapi_validator_binding_invalid.root.components.channelBindings.BindingWithNullProperty",
            line = 11,
        )
    }

    @Test
    fun `valid binding passes validation`() {
        val document = parse("validator/bindings/asyncapi_validator_binding_valid.yaml")
        val results = asyncApiValidator.validate(document)

        assertNoFindings(results)
    }

    @Test
    fun `invalid Kafka key schema fails validation`() {
        val results = validate("validator/bindings/asyncapi_validator_kafka_key_invalid.yaml")

        assertFinding(
            results,
            severity = ERROR,
            messageContains = "'minimum' (10.0) cannot be greater than 'maximum' (1.0)",
            sourceFile = "asyncapi_validator_kafka_key_invalid.yaml",
        )
    }
}
