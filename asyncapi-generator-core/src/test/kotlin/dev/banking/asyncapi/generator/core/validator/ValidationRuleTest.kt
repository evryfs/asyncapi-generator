package dev.banking.asyncapi.generator.core.validator

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ValidationRuleTest {
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

    @Test
    fun `rule names follow their codes`() {
        ValidationRule.entries.forEach { rule ->
            val expected =
                rule.code
                    .replace(Regex("^JSONSCHEMA-"), "SCHEMA-")
                    .replace(Regex("^(AAS3|GEN|ADV)-"), "")
                    .replace('-', '_')
            assertEquals(expected, rule.name, "Rule ${rule.code}")
        }
    }

    @Test
    fun `rules link to their specification section or their inventory row`() {
        val inventory = File("../docs/reference/validation-rules.md").readText()
        ValidationRule.entries.forEach { rule ->
            when (rule.code.substringBefore('-')) {
                "GEN", "ADV" -> {
                    assertTrue(rule.documentation.endsWith("validation-rules.md#${rule.code.lowercase()}"), rule.code)
                    assertTrue(
                        "<a id=\"${rule.code.lowercase()}\"></a>" in inventory,
                        "No inventory anchor for ${rule.code}",
                    )
                }
                "JSONSCHEMA" ->
                    assertTrue(
                        rule.documentation.startsWith("https://www.learnjsonschema.com/draft7/"),
                        rule.code,
                    )
                else ->
                    assertTrue(
                        "#" in rule.documentation || rule.documentation.endsWith("/bindings/kafka"),
                        "Specification link without a section for ${rule.code}",
                    )
            }
        }
    }
}
