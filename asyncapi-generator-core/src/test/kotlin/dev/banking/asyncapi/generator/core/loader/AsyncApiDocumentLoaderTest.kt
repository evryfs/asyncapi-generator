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
import dev.banking.asyncapi.generator.core.model.validator.ValidationFinding
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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
    fun `loads valid source incompatible Draft 7 schema constructs`() {
        val result = loader.load(TestResources.file("generator/source-incompatible-schema-features.yaml"))
        val components =
            (result.document.components as ComponentInterface.ComponentInline).component
        val schemas = requireNotNull(components.schemas)
        val tupleItems = (schemas.getValue("TupleItems") as SchemaInterface.SchemaInline).schema
        val falseItems = (schemas.getValue("FalseItems") as SchemaInterface.SchemaInline).schema
        val untypedEnum = (schemas.getValue("UntypedEnum") as SchemaInterface.SchemaInline).schema
        val ecmaPattern = (schemas.getValue("EcmaPattern") as SchemaInterface.SchemaInline).schema

        assertEquals(2, tupleItems.tupleItems?.size)
        assertEquals(null, tupleItems.items)
        assertEquals(false, (falseItems.items as SchemaInterface.BooleanSchema).value)
        assertEquals(listOf("open", 2, true, null), untypedEnum.enum)
        assertEquals(null, untypedEnum.type)
        assertEquals("(?<group_name>a)", ecmaPattern.pattern)
        assertEquals(emptyList(), result.warnings)
    }

    @Test
    fun `returns every canonical document and native schema source once`() {
        val result = loader.load(
            TestResources.file("parser/schemas/native-assets/asyncapi_external_native_schema_assets.yaml"),
        )

        assertEquals(
            setOf(
                "asyncapi_external_native_schema_assets.yaml",
                "user-created.avsc",
                "user-created.proto",
            ),
            result.sourceFiles.mapTo(mutableSetOf()) { it.name },
        )
        assertEquals(result.sourceFiles.size, result.sourceFiles.map { it.canonicalPath }.distinct().size)
        assertTrue(result.sourceFiles.all { it == it.canonicalFile })
        assertFailsWith<UnsupportedOperationException> {
            (result.sourceFiles as MutableSet<File>).add(tempDir.resolve("unexpected.yaml").toFile())
        }
    }

    @Test
    fun `returns each external warning once without logging from parser infrastructure`() {
        val external = tempDir.resolve("external-schema.yaml").toFile()
        external.writeText(
            """
            type: object
            required:
              - missingProperty
            properties: {}
            """.trimIndent(),
        )
        val main = tempDir.resolve("external-warning-main.yaml").toFile()
        main.writeText(
            """
            asyncapi: 3.0.0
            info:
              title: External warning
              version: 1.0.0
            components:
              schemas:
                External:
                  ${'$'}ref: ./external-schema.yaml
            """.trimIndent(),
        )

        val result = loader.load(main)

        assertEquals(1, result.warnings.size, result.warnings.joinToString("\n"))
        val warning = result.warnings.single()
        assertEquals("external-schema.yaml", warning.sourceLocation?.file?.name)
        assertEquals("external_schema.root.required", warning.path)
        assertTrue(result.formatWarnings().contains("external-schema.yaml"))
        assertFailsWith<UnsupportedOperationException> {
            (result.warnings as MutableList<ValidationFinding>).clear()
        }
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
    fun `rejects a known specification line without an implemented parser profile`() {
        val file =
            tempDir.writeTestFile(
                "unsupported-version.yaml",
                """
                asyncapi: 3.1.0
                info:
                  title: Unsupported version
                  version: 1.0.0
                """.trimIndent(),
            )

        val exception = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            loader.load(file)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnsupportedSpecificationVersion>(exception.diagnostic)

        assertTrue(diagnostic.knownVersionLine)
        assertEquals("unsupported_version.root.asyncapi", diagnostic.path)
        assertEquals(1, diagnostic.sourceLocation.line)
        assertEquals(11, diagnostic.sourceLocation.column)
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

        assertEquals(2, exception.errors.size)
        assertEquals(
            "asyncapi_validator_document_invalid.yaml",
            exception.errors.first().sourceLocation?.file?.name,
        )
        assertTrue(exception.message.orEmpty().contains("Validation failed with 2 error(s)"))
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

    @Test
    fun `supports concurrent loads without sharing parser state`() {
        val yamlFile = TestResources.file("parser/asyncapi/format-independent.yaml")
        val jsonFile = TestResources.file("parser/asyncapi/format-independent.json")
        val executor = Executors.newFixedThreadPool(4)

        try {
            val futures = (0 until 20).map { index ->
                executor.submit<AsyncApiDocumentLoadResult> {
                    loader.load(if (index % 2 == 0) yamlFile else jsonFile)
                }
            }
            val results = futures.map { future -> future.get(30, TimeUnit.SECONDS) }

            assertTrue(results.all { it.document == results.first().document })
            assertTrue(results.all { it.warnings.isEmpty() })
            assertTrue(results.all { it.sourceFiles.size == 1 })
        } finally {
            executor.shutdownNow()
        }
    }
}
