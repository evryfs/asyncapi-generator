package dev.banking.asyncapi.generator.core.parser.externaldocs

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.EXTERNAL_DOC
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class ExternalDocsParserTest {

    private val context = AsyncApiContext()
    private val parser = ExternalDocsParser(context)

    @Test
    fun `parse valid external docs`() {
        val file = TestResources.file("parser/externaldocs/asyncapi_parser_externaldocs_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val externalDocsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("externalDocs")

        val result = parser.parseMap(externalDocsNode)

        val externalDoc = assertIs<ExternalDocInterface.ExternalDocInline>(result["MyExternalDocs"]).externalDoc
        assertEquals("My API Documentation", externalDoc.description)
        assertEquals("https://example.com/api-docs", externalDoc.url)

        val reference =
            assertIs<ExternalDocInterface.ExternalDocReference>(result["RefExternalDocs"]).reference
        assertEquals("#/components/externalDocs/MyExternalDocs", reference.ref)
        assertEquals(EXTERNAL_DOC, reference.referenceCategoryKey)
    }

    @Test
    fun `parse external docs reports missing url`() {
        val file = TestResources.file("parser/externaldocs/asyncapi_parser_externaldocs_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val externalDocsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("externalDocs")
            .expectObject().required("MissingUrl")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(externalDocsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("url", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals(
            "asyncapi_parser_externaldocs_invalid.root.components.externalDocs.MissingUrl.url",
            diagnostic.path,
        )
        assertEquals("root.components.externalDocs.MissingUrl", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_externaldocs_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse external docs reports explicit null ref before inline parsing`() {
        val file = TestResources.file("parser/externaldocs/asyncapi_parser_externaldocs_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val externalDocsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("externalDocs")
            .expectObject().required("NullReference")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(externalDocsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NULL, diagnostic.actualType)
        assertNull(diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_externaldocs_invalid.root.components.externalDocs.NullReference.\$ref",
            diagnostic.path,
        )
        assertEquals("root.components.externalDocs.NullReference.\$ref", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_externaldocs_invalid.yaml", diagnostic.sourceLocation.file.name)
    }
}
