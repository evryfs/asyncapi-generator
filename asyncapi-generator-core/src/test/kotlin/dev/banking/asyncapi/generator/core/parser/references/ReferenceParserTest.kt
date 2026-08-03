package dev.banking.asyncapi.generator.core.parser.references

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.REFERENCE
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class ReferenceParserTest {

    private val context = AsyncApiContext()
    private val parser = ReferenceParser(context)

    @Test
    fun `parses one generic reference`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operations_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val referenceNode = ParserNodeFactory.root(document, context)
            .expectObject().required("operations")
            .expectObject().required("receiveLightMeasurement")
            .expectObject().required("channel")

        val reference = parser.parseElement(referenceNode, REFERENCE)

        assertEquals("#/channels/lightingMeasured", reference.ref)
        assertEquals(REFERENCE, reference.referenceCategoryKey)
    }

    @Test
    fun `parses a list of generic references`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operations_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val referencesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("operations")
            .expectObject().required("receiveLightMeasurement")
            .expectObject().required("messages")

        val references = parser.parseList(referencesNode, REFERENCE)

        assertEquals(listOf("#/components/messages/lightMeasured"), references.map { it.ref })
        assertEquals(listOf(REFERENCE), references.map { it.referenceCategoryKey })
    }

    @Test
    fun `rejects a generic category when loading an external fragment`() {
        val file = TestResources.file("parser/references/external/category-main.yaml")
        val document = DocumentReaderRegistry.read(file)
        val referenceNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("ExternalSchema")

        val error = assertFailsWith<IllegalArgumentException> {
            parser.parseElement(referenceNode, REFERENCE)
        }

        assertContains(error.message.orEmpty(), "Generic reference category 'REFERENCE' is not supported")
        assertContains(error.message.orEmpty(), "Assign a concrete ReferenceCategoryKey")
    }

    @Test
    fun `parse reference reports missing ref`() {
        val file = TestResources.file("parser/references/asyncapi_parser_reference_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val referenceNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("references")
            .expectObject().required("MissingReference")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(referenceNode, REFERENCE)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("\$ref", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals(
            "asyncapi_parser_reference_invalid.root.components.references.MissingReference.\$ref",
            diagnostic.path,
        )
        assertEquals("root.components.references.MissingReference", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_reference_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse reference reports explicit null ref`() {
        val file = TestResources.file("parser/references/asyncapi_parser_reference_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val referenceNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("references")
            .expectObject().required("NullReference")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(referenceNode, REFERENCE)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NULL, diagnostic.actualType)
        assertNull(diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_reference_invalid.root.components.references.NullReference.\$ref",
            diagnostic.path,
        )
        assertEquals("root.components.references.NullReference.\$ref", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_reference_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse reference list reports missing ref at element path`() {
        val file = TestResources.file("parser/references/asyncapi_parser_reference_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val referencesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("references")
            .expectObject().required("ReferenceList")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseList(referencesNode, REFERENCE)
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
