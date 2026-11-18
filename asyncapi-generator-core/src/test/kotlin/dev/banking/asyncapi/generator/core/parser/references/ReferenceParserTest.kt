package dev.banking.asyncapi.generator.core.parser.references

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.source.SourceReader
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ReferenceParserTest {
    private val context = AsyncApiContext()
    private val parser = ReferenceParser(context)

    @Test
    fun `parses a channel reference`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operations_valid.yaml")
        val document = SourceReader.read(file)
        val referenceNode =
            ParserNodeFactory
                .root(document, context)
                .expectObject()
                .required("operations")
                .expectObject()
                .required("receiveLightMeasurement")
                .expectObject()
                .required("channel")

        val reference = parser.parseChannel(referenceNode)

        assertEquals("#/channels/lightingMeasured", reference.ref)
        assertEquals(CHANNEL, reference.referenceCategoryKey)
    }

    @Test
    fun `parses a list of message references`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operations_valid.yaml")
        val document = SourceReader.read(file)
        val referencesNode =
            ParserNodeFactory
                .root(document, context)
                .expectObject()
                .required("operations")
                .expectObject()
                .required("receiveLightMeasurement")
                .expectObject()
                .required("messages")

        val references = parser.parseMessages(referencesNode)

        assertEquals(listOf("#/components/messages/lightMeasured"), references.map { it.ref })
        assertEquals(listOf(MESSAGE), references.map { it.referenceCategoryKey })
    }

    @Test
    fun `parse reference list reports missing ref at element path`() {
        val file = TestResources.file("parser/references/asyncapi_parser_reference_invalid.yaml")
        val document = SourceReader.read(file)
        val referencesNode =
            ParserNodeFactory
                .root(document, context)
                .expectObject()
                .required("components")
                .expectObject()
                .required("references")
                .expectObject()
                .required("ReferenceList")

        val error =
            assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
                parser.parseServers(referencesNode)
            }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("\$ref", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals(
            "asyncapi_parser_reference_invalid.root.components.references.ReferenceList[0].\$ref",
            diagnostic.path,
        )
        assertEquals("root.components.references.ReferenceList[0]", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_reference_invalid.yaml", diagnostic.sourceLocation.file.name)
    }
}
