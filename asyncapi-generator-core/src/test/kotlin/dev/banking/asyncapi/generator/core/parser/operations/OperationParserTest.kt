package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_TRAIT
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OperationParserTest {

    private val context = AsyncApiContext()
    private val parser = OperationParser(context)

    @Test
    fun `parse inline operations`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operations_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val operationsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("operations")

        val result = parser.parseMap(operationsNode)

        val receive = assertIs<OperationInterface.OperationInline>(result["receiveLightMeasurement"]).operation
        assertEquals("Receive lighting measurement", receive.title)
        assertEquals("receive", receive.action)
        assertEquals(
            "Inform about environmental lighting conditions of a particular streetlight.",
            receive.summary,
        )
        assertEquals("Receives one measured light level", receive.description)
        val receiveSecurity =
            assertIs<SecuritySchemeInterface.SecuritySchemeInline>(receive.security?.single()).security
        assertEquals("userPassword", receiveSecurity.type)
        assertEquals("#/channels/lightingMeasured", receive.channel?.ref)
        assertEquals(CHANNEL, receive.channel?.referenceCategoryKey)
        assertEquals(listOf("#/components/messages/lightMeasured"), receive.messages?.map { it.ref })
        assertEquals(listOf(MESSAGE), receive.messages?.map { it.referenceCategoryKey })

        val receiveTrait = assertIs<OperationTraitInterface.OperationTraitReference>(
            assertNotNull(receive.traits).single(),
        ).reference
        assertEquals("#/components/operationTraits/kafka", receiveTrait.ref)
        assertEquals(OPERATION_TRAIT, receiveTrait.referenceCategoryKey)

        val receiveExternalDoc =
            assertIs<ExternalDocInterface.ExternalDocInline>(receive.externalDocs).externalDoc
        assertEquals("https://example.com/api/oauth/dialog", receiveExternalDoc.url)

        val receiveReply = assertIs<OperationReplyInterface.OperationReplyInline>(receive.reply).operationReply
        assertNotNull(receiveReply.address)

        val turnOn = assertIs<OperationInterface.OperationInline>(result["turnOn"]).operation
        assertEquals("send", turnOn.action)
        assertEquals("#/channels/lightTurnOn", turnOn.channel?.ref)
        assertEquals(CHANNEL, turnOn.channel?.referenceCategoryKey)
        assertEquals(listOf("#/components/messages/turnOn"), turnOn.messages?.map { it.ref })
        assertEquals(listOf(MESSAGE), turnOn.messages?.map { it.referenceCategoryKey })

        val amqpBinding = assertIs<BindingInterface.BindingInline>(turnOn.bindings?.get("amqp")).binding
        assertEquals(mapOf("ack" to false), amqpBinding.content)

        val turnOnTrait = assertIs<OperationTraitInterface.OperationTraitReference>(
            assertNotNull(turnOn.traits).single(),
        ).reference
        assertEquals("#/components/operationTraits/kafka", turnOnTrait.ref)
        assertEquals(OPERATION_TRAIT, turnOnTrait.referenceCategoryKey)

        val tags = assertNotNull(turnOn.tags).map { assertIs<TagInterface.TagInline>(it).tag }
        assertEquals(listOf("user", "signup", "register"), tags.map { it.name })
        val registerExternalDoc =
            assertIs<ExternalDocInterface.ExternalDocInline>(tags[2].externalDocs).externalDoc
        assertEquals("https://example.com/docs/register", registerExternalDoc.url)
        assertEquals("Details about registration flows", registerExternalDoc.description)

        val turnOnExternalDoc = assertIs<ExternalDocInterface.ExternalDocInline>(turnOn.externalDocs).externalDoc
        assertEquals("https://example.com/api/oauth/dialog", turnOnExternalDoc.url)

        val turnOnReply = assertIs<OperationReplyInterface.OperationReplyInline>(turnOn.reply).operationReply
        assertNotNull(turnOnReply.address)
    }

    @Test
    fun `parses a referenced operation with its concrete category`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operations_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val operationsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("operations")

        val result = parser.parseMap(operationsNode)

        val reference = assertIs<OperationInterface.OperationReference>(result["referencedOperation"]).reference
        assertEquals("#/operations/receiveLightMeasurement", reference.ref)
        assertEquals(OPERATION, reference.referenceCategoryKey)
    }

    @Test
    fun `parse operation missing action reports the required member and source`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operations_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val operationsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("operations")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(operationsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("action", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals("asyncapi_parser_operations_invalid.root.operations.MissingAction.action", diagnostic.path)
        assertEquals("root.operations.MissingAction", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_operations_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse operation missing channel reports the required member and source`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operations_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val operationsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("operationCases")
            .expectObject().required("MissingChannel")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(operationsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("channel", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals(
            "asyncapi_parser_operations_invalid.root.operationCases.MissingChannel.invalidOperation.channel",
            diagnostic.path,
        )
        assertEquals(
            "root.operationCases.MissingChannel.invalidOperation",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_operations_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse operation with boolean action reports its expected type and source`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operations_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val operationsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("operationCases")
            .expectObject().required("BooleanAction")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(operationsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.BOOLEAN, diagnostic.actualType)
        assertEquals(false, diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_operations_invalid.root.operationCases.BooleanAction.invalidOperation.action",
            diagnostic.path,
        )
        assertEquals("root.operationCases.BooleanAction.invalidOperation.action", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_operations_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse operation with null reference reports its expected type and source`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operations_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val operationsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("operationCases")
            .expectObject().required("NullReference")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(operationsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NULL, diagnostic.actualType)
        assertNull(diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_operations_invalid.root.operationCases.NullReference.invalidOperation.\$ref",
            diagnostic.path,
        )
        assertEquals("root.operationCases.NullReference.invalidOperation.\$ref", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_operations_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse operation map from an array reports the container type and source`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operations_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val operationsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("operationCases")
            .expectObject().required("ArrayInsteadOfMap")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(operationsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.ARRAY, diagnostic.actualType)
        assertEquals(listOf(mapOf("action" to "send")), diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_operations_invalid.root.operationCases.ArrayInsteadOfMap",
            diagnostic.path,
        )
        assertEquals("root.operationCases.ArrayInsteadOfMap", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_operations_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse operation with inline message reports missing ref`() {
        val file = TestResources.file("parser/operations/asyncapi_validator_operations_inline_message_error.yaml")
        val document = DocumentReaderRegistry.read(file)
        val operationsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("operations")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(operationsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("\$ref", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals(
            "asyncapi_validator_operations_inline_message_error.root.operations.testOperation.messages[0].\$ref",
            diagnostic.path,
        )
        assertEquals("root.operations.testOperation.messages[0]", diagnostic.sourceLocation.path)
        assertEquals(
            "asyncapi_validator_operations_inline_message_error.yaml",
            diagnostic.sourceLocation.file.name,
        )
    }
}
