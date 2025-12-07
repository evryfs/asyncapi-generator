package com.tietoevry.banking.asyncapi.generator.core.validator.bindings

import com.tietoevry.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import com.tietoevry.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BindingValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid bindings trigger warnings`() {
        val document = parse("src/test/resources/validator/bindings/asyncapi_validator_binding_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        assertFalse(results.hasErrors(), "Expected no errors, but found: ${results.errors}")

        val warnings = results.warnings.map { it.message }
        assertEquals(3, warnings.size, "Expected 3 warnings for invalid bindings.")
    }

    @Test
    fun `valid binding passes validation`() {
        val document = parse("src/test/resources/validator/bindings/asyncapi_validator_binding_valid.yaml")
        val results = asyncApiValidator.validate(document)

        assertFalse(results.hasErrors(), "Expected no errors for valid binding.")
        assertFalse(results.hasWarnings(), "Expected no warnings for valid binding.")
    }
}
