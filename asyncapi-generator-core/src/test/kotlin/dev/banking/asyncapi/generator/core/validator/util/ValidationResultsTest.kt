package dev.banking.asyncapi.generator.core.validator.util

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
import dev.banking.asyncapi.generator.core.document.SourceLocation
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ValidationResultsTest {

    @Test
    fun `records error finding and exposes errors as filtered findings`() {
        val results = ValidationResults(AsyncApiContext())

        results.error(
            message = "The 'asyncapi' field is required.",
            line = 12,
            doc = "https://example.com/asyncapi",
            path = "streetlights.root.asyncapi",
        )

        val finding = results.findings.single()
        assertEquals(ERROR, finding.severity)
        assertEquals("The 'asyncapi' field is required.", finding.message)
        assertEquals(12, finding.line)
        assertEquals("https://example.com/asyncapi", finding.doc)
        assertNull(finding.sourceLocation)
        assertEquals("streetlights.root.asyncapi", finding.path)

        val error = results.errors.single()
        assertEquals(finding, error)
    }

    @Test
    fun `records warning finding and exposes warnings as filtered findings`() {
        val results = ValidationResults(AsyncApiContext())
        val sourceLocation = SourceLocation(
            sourceId = "streetlights",
            file = File("streetlights.yaml"),
            path = "streetlights.root.info.id",
            line = 8,
            column = 5,
        )

        results.warn(
            message = "It is recommended to use a URN.",
            doc = "https://example.com/urn",
            sourceLocation = sourceLocation,
        )

        val finding = results.findings.single()
        assertEquals(WARNING, finding.severity)
        assertEquals("It is recommended to use a URN.", finding.message)
        assertEquals(sourceLocation, finding.sourceLocation)
        assertEquals("streetlights.root.info.id", finding.path)
        assertEquals(8, finding.line)
        assertEquals("https://example.com/urn", finding.doc)

        val warning = results.warnings.single()
        assertEquals(finding, warning)
    }

    @Test
    fun `keeps findings in validation order`() {
        val results = ValidationResults(AsyncApiContext())

        results.warn("First warning", line = 2)
        results.error("Second error", line = 3)

        assertEquals(listOf(WARNING, ERROR), results.findings.map { it.severity })
        assertEquals(listOf("First warning", "Second error"), results.findings.map { it.message })
    }

    @Test
    fun `throws validation errors rendered from source locations`() {
        val asyncApiContext = AsyncApiContext()
        val file = File("streetlights.yaml")
        asyncApiContext.registerSource(
            file,
            """
            asyncapi: 2.6.0
            info:
              title: Streetlights
            """.trimIndent()
        )
        val sourceLocation = SourceLocation(
            sourceId = "streetlights",
            file = file,
            path = "streetlights.root.asyncapi",
            line = 1,
            column = 1,
        )
        asyncApiContext.registerSourceLocation(sourceLocation.path, sourceLocation)

        val results = ValidationResults(asyncApiContext)
        results.error(
            message = "Unsupported AsyncAPI version.",
            doc = "https://example.com/asyncapi",
            sourceLocation = sourceLocation,
        )

        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }

        assertEquals(results.errors, exception.errors)
        val message = exception.message.orEmpty()
        assertContains(message, "Validation failed with 1 error(s):")
        assertContains(message, ">> Unsupported AsyncAPI version.")
        assertContains(message, "streetlights.yaml (streetlights.root.asyncapi)")
        assertContains(message, "→    1 | asyncapi: 2.6.0")
        assertContains(message, "See documentation: https://example.com/asyncapi")
    }
}
