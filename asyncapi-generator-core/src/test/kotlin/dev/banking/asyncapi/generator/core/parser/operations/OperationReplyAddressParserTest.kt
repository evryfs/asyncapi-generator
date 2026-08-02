package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_REPLY_ADDRESS
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class OperationReplyAddressParserTest {

    private val context = AsyncApiContext()
    private val parser = OperationReplyAddressParser(context)

    @Test
    fun `parses an inline operation reply address`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operations_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val addressNode = ParserNodeFactory.root(document, context)
            .expectObject().required("operations")
            .expectObject().required("receiveLightMeasurement")
            .expectObject().required("reply")
            .expectObject().required("address")

        val address = assertIs<OperationReplyAddressInterface.OperationReplyAddressInline>(
            parser.parseElement(addressNode),
        ).operationReplyAddress

        assertEquals("\$message.header#/replyTo", address.location)
    }

    @Test
    fun `parses a referenced operation reply address with its concrete category`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operations_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val addressNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("replyAddresses")
            .expectObject().required("referencedReplyAddress")

        val reference = assertIs<OperationReplyAddressInterface.OperationReplyAddressReference>(
            parser.parseElement(addressNode),
        ).reference

        assertEquals("#/components/replyAddresses/standardReplyAddress", reference.ref)
        assertEquals(OPERATION_REPLY_ADDRESS, reference.referenceCategoryKey)
    }

    @Test
    fun `parse operation reply address missing location reports the required member and source`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operation_reply_address_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val addressNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("operationReplyAddressCases")
            .expectObject().required("MissingLocation")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(addressNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("location", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals(
            "asyncapi_parser_operation_reply_address_invalid.root.components.operationReplyAddressCases.MissingLocation.location",
            diagnostic.path,
        )
        assertEquals("root.components.operationReplyAddressCases.MissingLocation", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_operation_reply_address_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse operation reply address with boolean location reports its expected type and source`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operation_reply_address_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val addressNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("operationReplyAddressCases")
            .expectObject().required("BooleanLocation")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(addressNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.BOOLEAN, diagnostic.actualType)
        assertEquals(true, diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_operation_reply_address_invalid.root.components.operationReplyAddressCases.BooleanLocation.location",
            diagnostic.path,
        )
        assertEquals(
            "root.components.operationReplyAddressCases.BooleanLocation.location",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_operation_reply_address_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse operation reply address with null reference reports its expected type and source`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operation_reply_address_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val addressNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("operationReplyAddressCases")
            .expectObject().required("NullReference")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(addressNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NULL, diagnostic.actualType)
        assertNull(diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_operation_reply_address_invalid.root.components.operationReplyAddressCases.NullReference.\$ref",
            diagnostic.path,
        )
        assertEquals("root.components.operationReplyAddressCases.NullReference.\$ref", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_operation_reply_address_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse operation reply address map from an array reports the container type and source`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operation_reply_address_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val addressesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("operationReplyAddressCases")
            .expectObject().required("ArrayInsteadOfMap")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(addressesNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.ARRAY, diagnostic.actualType)
        assertEquals(listOf(mapOf("location" to "\$message.header#/replyTo")), diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_operation_reply_address_invalid.root.components.operationReplyAddressCases.ArrayInsteadOfMap",
            diagnostic.path,
        )
        assertEquals("root.components.operationReplyAddressCases.ArrayInsteadOfMap", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_operation_reply_address_invalid.yaml", diagnostic.sourceLocation.file.name)
    }
}
