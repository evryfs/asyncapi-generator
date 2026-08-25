package dev.banking.asyncapi.generator.core.validator.util

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.document.SourceLocation
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationConcern.ADVISORY
import dev.banking.asyncapi.generator.core.model.validator.ValidationConcern.SPECIFICATION
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.DOCUMENT_ID_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.DOCUMENT_ID_URN_RECOMMENDED
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationReportTest {

    @Test
    fun `report exposes stable rule metadata and source-derived coordinates`() {
        val sourceLocation = SourceLocation(
            sourceId = "streetlights",
            file = File("streetlights.yaml"),
            path = "streetlights.root.id",
            line = 8,
            column = 5,
        )
        val collector = ValidationCollector()

        collector.error(DOCUMENT_ID_FORMAT, "Invalid identifier.", sourceLocation)
        collector.warn(DOCUMENT_ID_URN_RECOMMENDED, "Prefer a URN.", sourceLocation)
        val report = collector.report()

        val error = report.errors.single()
        assertEquals("AAS3-DOCUMENT-ID-FORMAT", error.code)
        assertEquals(SPECIFICATION, error.concern)
        assertEquals(ERROR, error.severity)
        assertEquals(sourceLocation, error.sourceLocation)
        assertEquals("streetlights.root.id", error.path)
        assertEquals(8, error.line)
        assertEquals(DOCUMENT_ID_FORMAT.documentation, error.documentation)

        val warning = report.warnings.single()
        assertEquals("ADV-DOCUMENT-ID-URN", warning.code)
        assertEquals(ADVISORY, warning.concern)
        assertEquals(WARNING, warning.severity)
        assertEquals(listOf(error, warning), report.findings)
        assertTrue(report.hasErrors())
        assertTrue(report.hasWarnings())
    }

    @Test
    fun `report is a snapshot of collector state`() {
        val collector = ValidationCollector()
        collector.error(DOCUMENT_ID_FORMAT, "First error.")
        val report = collector.report()

        collector.warn(DOCUMENT_ID_URN_RECOMMENDED, "Later warning.")

        assertEquals(1, report.findings.size)
        assertTrue(report.hasErrors())
        assertFalse(report.hasWarnings())
    }

    @Test
    fun `reporter throws errors rendered from source locations`() {
        val context = AsyncApiContext()
        val file = File("streetlights.yaml")
        context.sourceTracking.registerSource(
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
        context.sourceTracking.registerLocation(sourceLocation.path, sourceLocation)
        val collector = ValidationCollector()
        collector.error(DOCUMENT_ID_FORMAT, "Unsupported AsyncAPI version.", sourceLocation)
        val report = collector.report()

        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            ValidationReporter(context).throwErrors(report)
        }

        assertEquals(report.errors, exception.errors)
        val message = exception.message.orEmpty()
        assertContains(message, "Validation failed with 1 error(s):")
        assertContains(message, ">> Unsupported AsyncAPI version.")
        assertContains(message, "streetlights.yaml (streetlights.root.asyncapi)")
        assertContains(message, "→    1 | asyncapi: 2.6.0")
        assertContains(message, "See documentation: ${DOCUMENT_ID_FORMAT.documentation}")
    }
}
