package dev.banking.asyncapi.generator.core.validator.components

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.COMPONENT_NAME_FORMAT
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ComponentValidatorTest : AbstractValidatorTest() {

    @Test
    fun `component names may contain dots`() {
        val results = validate("validator/components/asyncapi_validator_component_names_valid.yaml")

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `every component map checks its key format`() {
        val results = validate("validator/components/asyncapi_validator_component_names_invalid.yaml")

        assertEquals(2, results.findings.size)
        results.findings.forEach { finding ->
            assertEquals(COMPONENT_NAME_FORMAT.code, finding.code)
            assertEquals(COMPONENT_NAME_FORMAT.severity, finding.severity)
            assertEquals(COMPONENT_NAME_FORMAT.concern, finding.concern)
            assertEquals("asyncapi_validator_component_names_invalid.yaml", finding.sourceLocation?.file?.name)
        }
        assertEquals(
            setOf(
                "asyncapi_validator_component_names_invalid.root.components.messages",
                "asyncapi_validator_component_names_invalid.root.components.schemas",
            ),
            results.findings.map { it.path }.toSet(),
        )
    }
}
