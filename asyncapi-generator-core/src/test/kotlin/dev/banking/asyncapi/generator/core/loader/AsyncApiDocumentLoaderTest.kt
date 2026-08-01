package dev.banking.asyncapi.generator.core.loader

import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.fixtures.writeTestFile
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AsyncApiDocumentLoaderTest {

    @TempDir
    lateinit var tempDir: Path

    private val loader = AsyncApiDocumentLoader()

    @Test
    fun `loads equivalent yaml and json documents into the same validated model`() {
        val yaml = loader.load(TestResources.file("parser/asyncapi/format-independent.yaml"))
        val json = loader.load(TestResources.file("parser/asyncapi/format-independent.json"))

        assertEquals(yaml.document, json.document)
        assertEquals(emptyList(), yaml.warnings)
        assertEquals(emptyList(), json.warnings)
    }

    @Test
    fun `loads and resolves external schema references`() {
        val result = loader.load(TestResources.file("parser/references/external/main.yaml"))
        val components =
            (result.document.components as ComponentInterface.ComponentInline).component
        val schemas = requireNotNull(components.schemas)
        val reference =
            (schemas.getValue("RootReference") as SchemaInterface.SchemaReference).reference

        assertEquals("root schema target", reference.requireModel<Schema>().description)
    }

    @Test
    fun `preserves source aware parse failures`() {
        val file =
            tempDir.writeTestFile(
                "invalid-type.yaml",
                """
                asyncapi: false
                info:
                  title: Invalid type
                  version: 1.0.0
                """.trimIndent(),
            )

        val exception = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            loader.load(file)
        }

        assertEquals("invalid_type.root.asyncapi", exception.diagnostic.path)
        assertEquals("invalid-type.yaml", exception.diagnostic.sourceLocation.file.name)
        assertEquals(1, exception.diagnostic.sourceLocation.line)
    }

    @Test
    fun `parser rejects a syntactically valid array root with a source aware diagnostic`() {
        val file =
            tempDir.writeTestFile(
                "array-root.yaml",
                """
                - asyncapi
                - info
                """.trimIndent(),
            )

        val exception = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            loader.load(file)
        }

        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(exception.diagnostic)
        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.ARRAY, diagnostic.actualType)
        assertEquals(listOf("asyncapi", "info"), diagnostic.actualValue)
        assertEquals("array_root.root", diagnostic.path)
        assertEquals("root", diagnostic.sourceLocation.path)
        assertEquals("array-root.yaml", diagnostic.sourceLocation.file.name)
        assertEquals(1, diagnostic.sourceLocation.line)
        assertEquals(1, diagnostic.sourceLocation.column)
    }

    @Test
    fun `throws source aware semantic validation failures`() {
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            loader.load(
                TestResources.file("validator/asyncapi/asyncapi_validator_document_invalid.yaml"),
            )
        }

        assertEquals(3, exception.errors.size)
        assertEquals(
            "asyncapi_validator_document_invalid.yaml",
            exception.errors.first().sourceLocation?.file?.name,
        )
        assertTrue(exception.message.orEmpty().contains("Validation failed with 3 error(s)"))
    }

    @Test
    fun `returns warnings with source aware formatting`() {
        val file =
            tempDir.writeTestFile(
                "warning.yaml",
                """
                asyncapi: 3.0.0
                id: https://example.com/events
                info:
                  title: Warning document
                  version: 1.0.0
                """.trimIndent(),
            )

        val result = loader.load(file)

        assertEquals(1, result.warnings.size)
        assertEquals("warning.yaml", result.warnings.single().sourceLocation?.file?.name)
        assertEquals("warning.root.id", result.warnings.single().path)
        assertTrue(result.formatWarnings().contains("warning.yaml (warning.root.id)"))
        assertTrue(result.formatWarnings().contains("https://example.com/events"))
    }

    @Test
    fun `uses an isolated parser context for each load`() {
        val warningFile =
            tempDir.writeTestFile(
                "first.yaml",
                """
                asyncapi: 3.0.0
                id: https://example.com/events
                info:
                  title: First document
                  version: 1.0.0
                """.trimIndent(),
            )
        val validFile =
            tempDir.writeTestFile(
                "second.yaml",
                """
                asyncapi: 3.0.0
                info:
                  title: Second document
                  version: 1.0.0
                """.trimIndent(),
            )

        assertEquals(1, loader.load(warningFile).warnings.size)
        assertEquals(emptyList(), loader.load(validFile).warnings)
    }
}
