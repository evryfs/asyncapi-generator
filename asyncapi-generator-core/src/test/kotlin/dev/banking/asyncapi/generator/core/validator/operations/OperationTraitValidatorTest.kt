package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_TYPE_VALUE
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OperationTraitValidatorTest : AbstractValidatorTest() {

    @Test
    fun `accepts operation trait security alternatives and references`() {
        val results = validate("validator/operations/asyncapi_validator_operation_trait_security_valid.yaml")

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `validates each security alternative in an operation trait`() {
        val results = validate("validator/operations/asyncapi_validator_operation_trait_security_invalid.yaml")

        assertEquals(1, results.errors.size)
        assertEquals(0, results.warnings.size)
        val invalidSecurity = results.findings.single()
        assertEquals(SECURITY_TYPE_VALUE.code, invalidSecurity.code)
        assertEquals(SECURITY_TYPE_VALUE.severity, invalidSecurity.severity)
        assertEquals(SECURITY_TYPE_VALUE.concern, invalidSecurity.concern)
        assertEquals(
            "asyncapi_validator_operation_trait_security_invalid.yaml",
            invalidSecurity.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_operation_trait_security_invalid.root.components.operationTraits.InvalidSecurity.security[0].type",
            invalidSecurity.path,
        )
        assertEquals(9, invalidSecurity.line)
    }
}
