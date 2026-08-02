package dev.banking.asyncapi.generator.core.validator

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidationRuleTest {

    @Test
    fun `rule codes are unique and identify their ownership category`() {
        val rules = ValidationRule.entries

        assertEquals(rules.size, rules.map(ValidationRule::code).distinct().size)
        rules.forEach { rule ->
            assertTrue(rule.documentation.isNotBlank(), "Missing documentation for ${rule.code}")
            assertTrue(
                rule.code.startsWith("AAS3-") ||
                    rule.code.startsWith("JSONSCHEMA-") ||
                    rule.code.startsWith("GEN-") ||
                    rule.code.startsWith("ADV-"),
                "Unexpected rule-code namespace: ${rule.code}",
            )
        }
    }
}
