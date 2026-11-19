package dev.banking.asyncapi.generator.core.validator.util

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_ACTION_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REPLY_ADDRESS_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SECURITY_TYPE_REQUIRED
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class RequiredValueTest : AbstractValidatorTest() {
    @Test
    fun `required values that are only whitespace are reported as missing`() {
        val codes = validate("validator/util/asyncapi_validator_blank_required_values.yaml").errors.map { it.code }

        listOf(OPERATION_ACTION_REQUIRED, REPLY_ADDRESS_REQUIRED, SECURITY_TYPE_REQUIRED).forEach { rule ->
            assertTrue(rule.code in codes, "${rule.code} not reported in $codes")
        }
    }
}
