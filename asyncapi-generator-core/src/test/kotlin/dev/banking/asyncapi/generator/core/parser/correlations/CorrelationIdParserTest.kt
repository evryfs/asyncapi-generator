package dev.banking.asyncapi.generator.core.parser.correlations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class CorrelationIdParserTest {

    private val context = AsyncApiContext()
    private val parser = CorrelationIdParser(context)

    @Test
    fun `parse inline correlation ID`() {
        val file = TestResources.file("parser/correlations/asyncapi_parser_correlationid_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val correlationIdNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("correlationIds")
            .expectObject().required("MyCorrelationId")

        val result = assertIs<CorrelationIdInterface.CorrelationIdInline>(
            parser.parseElement(correlationIdNode),
        ).correlationId

        assertEquals("\$message.header#/correlationId", result.location)
        assertEquals("My custom correlation ID", result.description)
    }

    @Test
    fun `parse correlation ID reports missing location`() {
        val file = TestResources.file("parser/correlations/asyncapi_parser_correlationid_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val correlationIdNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("correlationIds")
            .expectObject().required("MissingLocationId")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(correlationIdNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("location", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals(
            "asyncapi_parser_correlationid_invalid.root.components.correlationIds.MissingLocationId.location",
            diagnostic.path,
        )
        assertEquals("root.components.correlationIds.MissingLocationId", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_correlationid_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse correlation ID reports non-string ref before inline parsing`() {
        val file = TestResources.file("parser/correlations/asyncapi_parser_correlationid_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val correlationIdNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("correlationIds")
            .expectObject().required("NumericReference")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(correlationIdNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NUMBER, diagnostic.actualType)
        assertEquals(42, diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_correlationid_invalid.root.components.correlationIds.NumericReference.\$ref",
            diagnostic.path,
        )
        assertEquals("root.components.correlationIds.NumericReference.\$ref", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_correlationid_invalid.yaml", diagnostic.sourceLocation.file.name)
    }
}
