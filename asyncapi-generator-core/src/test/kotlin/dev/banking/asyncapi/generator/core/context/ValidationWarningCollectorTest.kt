package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.model.validator.ValidationConcern
import dev.banking.asyncapi.generator.core.model.validator.ValidationFinding
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ValidationWarningCollectorTest {

    private val collector = ValidationWarningCollector()

    @Test
    fun `mergeWith returns root warnings when no external warnings collected`() {
        val root = listOf(finding("W001"))

        assertEquals(1, collector.mergeWith(root).size)
        assertEquals("W001", collector.mergeWith(root)[0].code)
    }

    @Test
    fun `mergeWith deduplicates warnings with same identity`() {
        val warning = finding("W001")

        collector.collect(listOf(warning))
        collector.collect(listOf(warning))

        assertEquals(1, collector.mergeWith(emptyList()).size)
    }

    @Test
    fun `mergeWith preserves distinct warnings`() {
        val first = finding("W001")
        val second = finding("W002")

        collector.collect(listOf(first))

        val result = collector.mergeWith(listOf(second))

        assertEquals(2, result.size)
    }

    @Test
    fun `mergeWith does not duplicate external and root warnings with same identity`() {
        val warning = finding("W001")

        collector.collect(listOf(warning))

        val result = collector.mergeWith(listOf(warning))

        assertEquals(1, result.size)
    }

    private fun finding(code: String) = ValidationFinding(
        code = code,
        concern = ValidationConcern.SPECIFICATION,
        severity = ValidationSeverity.WARNING,
        message = "test",
        documentation = "test",
    )
}
