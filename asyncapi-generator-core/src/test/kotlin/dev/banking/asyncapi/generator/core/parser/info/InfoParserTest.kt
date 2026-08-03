package dev.banking.asyncapi.generator.core.parser.info

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InfoParserTest {

    private val context = AsyncApiContext()
    private val parser = InfoParser(context)

    @Test
    fun `parses every info field and extension`() {
        val file = TestResources.file("parser/info/asyncapi_parser_info_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val infoNode = ParserNodeFactory.root(document, context)
            .expectObject().required("info")

        val result = parser.parseMap(infoNode)

        assertEquals("Simple Info Test", result.title)
        assertEquals("1.2.3", result.version)
        assertEquals("A simple description", result.description)
        assertEquals("https://example.com/terms", result.termsOfService)
        assertEquals("Support", result.contact?.name)
        assertEquals("https://support.example.com", result.contact?.url)
        assertEquals("support@example.com", result.contact?.email)
        assertEquals("Apache 2.0", result.license?.name)
        assertEquals("https://www.apache.org/licenses/LICENSE-2.0", result.license?.url)

        val tag = assertIs<TagInterface.TagInline>(assertNotNull(result.tags).single()).tag
        assertEquals("general", tag.name)
        assertEquals("General tag", tag.description)

        val externalDoc = assertIs<ExternalDocInterface.ExternalDocInline>(result.externalDocs).externalDoc
        assertEquals("https://example.com/docs", externalDoc.url)
        assertEquals("Documentation", externalDoc.description)
        assertEquals(mapOf("x-custom-extension" to "value"), result.extensions)
    }

    @Test
    fun `parse info reports missing title`() {
        val file = TestResources.file("parser/info/asyncapi_parser_info_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val infoNode = ParserNodeFactory.root(document, context)
            .expectObject().required("info")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(infoNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("title", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals("asyncapi_parser_info_invalid.root.info.title", diagnostic.path)
        assertEquals("root.info", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_info_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse info reports missing version`() {
        val file = TestResources.file("parser/info/asyncapi_parser_info_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val infoNode = ParserNodeFactory.root(document, context)
            .expectObject().required("infoMissingVersion")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(infoNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("version", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals("asyncapi_parser_info_invalid.root.infoMissingVersion.version", diagnostic.path)
        assertEquals("root.infoMissingVersion", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_info_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse info reports missing license name`() {
        val file = TestResources.file("parser/info/asyncapi_parser_info_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val infoNode = ParserNodeFactory.root(document, context)
            .expectObject().required("infoMissingLicenseName")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(infoNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("name", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals("asyncapi_parser_info_invalid.root.infoMissingLicenseName.license.name", diagnostic.path)
        assertEquals("root.infoMissingLicenseName.license", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_info_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse info preserves nested and null extensions in source order`() {
        val file = TestResources.file("parser/info/asyncapi_parser_info_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val infoNode = ParserNodeFactory.root(document, context)
            .expectObject().required("infoWithExtensions")

        val extensions = parser.parseMap(infoNode).extensions

        assertEquals(
            linkedMapOf(
                "x-object" to mapOf(
                    "items" to listOf(
                        "alpha",
                        mapOf("enabled" to true, "limit" to 2),
                    )
                ),
                "x-null" to null,
                "x-array" to listOf(1, "two", false),
            ),
            extensions,
        )
        assertEquals(listOf("x-object", "x-null", "x-array"), extensions?.keys?.toList())
    }

    @Test
    fun `parse info without extensions returns null extensions`() {
        val file = TestResources.file("parser/info/asyncapi_parser_info_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val infoNode = ParserNodeFactory.root(document, context)
            .expectObject().required("infoWithoutExtensions")

        assertNull(parser.parseMap(infoNode).extensions)
    }

}
