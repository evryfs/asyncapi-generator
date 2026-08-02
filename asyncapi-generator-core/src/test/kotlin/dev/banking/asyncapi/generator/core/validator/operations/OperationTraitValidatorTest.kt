package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_TRAIT_EMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_TYPE_VALUE
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OperationTraitValidatorTest : AbstractValidatorTest() {

    @Test
    fun `accepts operation trait security alternatives and references`() {
        val results = validate("validator/operations/asyncapi_validator_operation_trait_security_valid.yaml")

        assertNoFindings(results)
    }

    @Test
    fun `validates each security alternative in an operation trait`() {
        val results = validate("validator/operations/asyncapi_validator_operation_trait_security_invalid.yaml")

        assertEquals(1, results.errors.size)
        assertEquals(1, results.warnings.size)
        assertRule(
            results,
            OPERATION_TRAIT_EMPTY,
            path = "asyncapi_validator_operation_trait_security_invalid.root.components.operationTraits.EmptyTrait",
            line = 7,
        )
        assertRule(
            results,
            SECURITY_TYPE_VALUE,
            path = "asyncapi_validator_operation_trait_security_invalid.root.components." +
                "operationTraits.InvalidSecurity.security[0].type",
            line = 10,
        )
    }
}
