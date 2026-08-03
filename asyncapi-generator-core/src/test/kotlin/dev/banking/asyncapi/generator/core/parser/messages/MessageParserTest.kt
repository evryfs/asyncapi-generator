package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CORRELATION_ID
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE_TRAIT
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SCHEMA
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MessageParserTest {

    private val context = AsyncApiContext()
    private val parser = MessageParser(context)

    @Test
    fun `parse inline and referenced messages`() {
        val file = TestResources.file("parser/messages/asyncapi_parser_message_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val messagesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messages")

        val result = parser.parseMap(messagesNode)

        val lightMeasured = assertIs<MessageInterface.MessageInline>(result["lightMeasured"]).message
        assertEquals("lightMeasured", lightMeasured.name)
        assertEquals("Light measured", lightMeasured.title)
        assertEquals(
            "Inform about environmental lighting conditions of a particular streetlight.",
            lightMeasured.summary,
        )
        assertEquals("A measured light level event", lightMeasured.description)
        assertEquals("application/json", lightMeasured.contentType)
        val messageCorrelation =
            assertIs<CorrelationIdInterface.CorrelationIdInline>(lightMeasured.correlationId).correlationId
        assertEquals("\$message.header#/correlationId", messageCorrelation.location)
        val messageExternalDocs =
            assertIs<ExternalDocInterface.ExternalDocInline>(lightMeasured.externalDocs).externalDoc
        assertEquals("https://example.com/docs/message", messageExternalDocs.url)

        val tags = assertNotNull(lightMeasured.tags).map { assertIs<TagInterface.TagInline>(it).tag }
        assertEquals(listOf("telemetry", "light"), tags.map { it.name })
        assertEquals(
            listOf("Messages about environmental sensors", "Messages about light levels"),
            tags.map { it.description },
        )

        val traitReference = assertIs<MessageTraitInterface.ReferenceMessageTrait>(
            assertNotNull(lightMeasured.traits).single(),
        ).reference
        assertEquals("#/components/messageTraits/commonHeaders", traitReference.ref)
        assertEquals(MESSAGE_TRAIT, traitReference.referenceCategoryKey)

        val headers = assertIs<SchemaInterface.SchemaInline>(lightMeasured.headers).schema
        assertEquals("object", headers.type)
        assertEquals(listOf("correlationId"), headers.required)
        val headerProperties = assertNotNull(headers.properties)
        val correlationId = assertIs<SchemaInterface.SchemaInline>(headerProperties["correlationId"]).schema
        assertEquals("string", correlationId.type)
        assertEquals("Correlation ID set by application", correlationId.description)
        val applicationInstanceId =
            assertIs<SchemaInterface.SchemaInline>(headerProperties["applicationInstanceId"]).schema
        assertEquals("string", applicationInstanceId.type)
        assertEquals(
            "Unique identifier for a given instance of the publishing application",
            applicationInstanceId.description,
        )

        val payload = assertIs<SchemaInterface.SchemaReference>(lightMeasured.payload).reference
        assertEquals("#/components/schemas/lightMeasuredPayload", payload.ref)
        assertEquals(SCHEMA, payload.referenceCategoryKey)

        val examples = assertNotNull(lightMeasured.examples)
        assertEquals(listOf("lightMeasurementExample", "lightMeasurementExample2"), examples.map { it.name })
        assertEquals(
            listOf(
                "Example of light measurement payload",
                "Example of light measurement payload 2",
            ),
            examples.map { it.summary },
        )

        val turnOnOff = assertIs<MessageInterface.MessageInline>(result["turnOnOff"]).message
        assertEquals("turnOnOff", turnOnOff.name)
        assertEquals("Turn on/off", turnOnOff.title)
        assertEquals("Command a particular streetlight to turn the lights on or off.", turnOnOff.summary)
        val amqpBinding = assertIs<BindingInterface.BindingInline>(turnOnOff.bindings?.get("amqp")).binding
        assertEquals(
            mapOf("contentEncoding" to "gzip", "messageType" to "turnOnCommand"),
            amqpBinding.content,
        )
        val turnOnExample = assertNotNull(turnOnOff.examples).single()
        assertEquals("turnOnExample", turnOnExample.name)
        val turnOnTrait = assertIs<MessageTraitInterface.ReferenceMessageTrait>(
            assertNotNull(turnOnOff.traits).single(),
        ).reference
        assertEquals("#/components/messageTraits/commonHeaders", turnOnTrait.ref)
        assertEquals(MESSAGE_TRAIT, turnOnTrait.referenceCategoryKey)

        val referencedMessage =
            assertIs<MessageInterface.MessageReference>(result["referencedMessage"]).reference
        assertEquals("#/components/messages/lightMeasured", referencedMessage.ref)
        assertEquals(MESSAGE, referencedMessage.referenceCategoryKey)
    }

    @Test
    fun `parse messages with referenced payload and correlation ID`() {
        val file = TestResources.file("parser/messages/asyncapi_parser_message_edge_cases.yaml")
        val document = DocumentReaderRegistry.read(file)
        val messagesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messages")

        val messages = parser.parseMap(messagesNode)

        val refPayload = assertIs<MessageInterface.MessageInline>(messages["RefPayload"]).message
        assertEquals("RefPayload", refPayload.name)
        val payloadReference = assertIs<SchemaInterface.SchemaReference>(refPayload.payload).reference
        assertEquals("#/components/schemas/MySchema", payloadReference.ref)
        assertEquals(SCHEMA, payloadReference.referenceCategoryKey)

        val refCorrelation = assertIs<MessageInterface.MessageInline>(messages["RefCorrelationId"]).message
        assertEquals("RefCorrelationId", refCorrelation.name)
        val correlationReference =
            assertIs<CorrelationIdInterface.CorrelationIdReference>(refCorrelation.correlationId).reference
        assertEquals("#/components/correlationIds/myId", correlationReference.ref)
        assertEquals(CORRELATION_ID, correlationReference.referenceCategoryKey)
    }

    @Test
    fun `parse reference object ignores sibling members`() {
        val file = TestResources.file("parser/messages/asyncapi_parser_message_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val messagesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messages")

        val reference = assertIs<MessageInterface.MessageReference>(
            parser.parseMap(messagesNode).getValue("referencedMessage"),
        ).reference

        assertEquals("#/components/messages/lightMeasured", reference.ref)
        assertEquals(MESSAGE, reference.referenceCategoryKey)
    }

    @Test
    fun `parse message with empty payload and inline trait`() {
        val file = TestResources.file("parser/messages/asyncapi_parser_message_edge_cases.yaml")
        val document = DocumentReaderRegistry.read(file)
        val messagesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messages")

        val messages = parser.parseMap(messagesNode)

        val emptyPayload = assertIs<MessageInterface.MessageInline>(messages["EmptyPayloadMessage"]).message
        assertEquals("empty", emptyPayload.name)
        assertNull(emptyPayload.payload)

        val inlineTrait = assertIs<MessageInterface.MessageInline>(messages["InlineTraitMessage"]).message
        assertEquals("InlineTraitMessage", inlineTrait.name)
        val trait = assertIs<MessageTraitInterface.InlineMessageTrait>(
            assertNotNull(inlineTrait.traits).single(),
        ).trait
        val headers = assertIs<SchemaInterface.SchemaInline>(trait.headers).schema
        assertEquals("string", headers.type)
    }

    @Test
    fun `rejects unknown message members and accepts specification extensions`() {
        val file = TestResources.file("parser/messages/asyncapi_parser_message_member_policy.yaml")
        val document = DocumentReaderRegistry.read(file)
        val messageCases = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messageCases")
            .expectObject()

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(messageCases.required("UnknownMember"))
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedObjectMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_OBJECT_MEMBER, diagnostic.category)
        assertEquals("ImportedMessage", diagnostic.memberName)
        assertEquals(
            "asyncapi_parser_message_member_policy.root.components.messageCases." +
                "UnknownMember.invalidMessage.ImportedMessage",
            diagnostic.path,
        )
        assertEquals("asyncapi_parser_message_member_policy.yaml", diagnostic.sourceLocation.file.name)

        val message = assertIs<MessageInterface.MessageInline>(
            parser.parseMap(messageCases.required("SpecificationExtension")).getValue("validMessage"),
        ).message
        assertEquals("valid", message.name)
    }
}
