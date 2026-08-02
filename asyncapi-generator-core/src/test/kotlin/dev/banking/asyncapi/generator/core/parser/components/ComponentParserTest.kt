package dev.banking.asyncapi.generator.core.parser.components

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ComponentParserTest {

    private val context = AsyncApiContext()
    private val parser = ComponentParser(context)

    @Test
    fun `parses every supported component category`() {
        val file = TestResources.file("parser/components/asyncapi_parser_components_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val componentsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")

        val component = assertIs<ComponentInterface.ComponentInline>(parser.parseElement(componentsNode)).component

        assertNotNull(component.schemas, "Schemas should be parsed")
        assertTrue(component.schemas.containsKey("MySchema"))

        assertNotNull(component.servers, "Servers should be parsed")
        assertTrue(component.servers.containsKey("MyServer"))

        assertNotNull(component.serverVariables, "Server Variables should be parsed")
        assertTrue(component.serverVariables.containsKey("MyServerVariable"))

        assertNotNull(component.channels, "Channels should be parsed")
        assertTrue(component.channels.containsKey("MyChannel"))

        assertNotNull(component.operations, "Operations should be parsed")
        assertTrue(component.operations.containsKey("MyOperation"))

        assertNotNull(component.messages, "Messages should be parsed")
        assertTrue(component.messages.containsKey("MyMessage"))

        assertNotNull(component.securitySchemes, "Security Schemes should be parsed")
        assertTrue(component.securitySchemes.containsKey("MySecurity"))

        assertNotNull(component.parameters, "Parameters should be parsed")
        assertTrue(component.parameters.containsKey("MyParam"))

        assertNotNull(component.correlationIds, "Correlation IDs should be parsed")
        assertTrue(component.correlationIds.containsKey("MyCorrelation"))

        assertNotNull(component.replies, "Operation Replies should be parsed")
        assertTrue(component.replies.containsKey("MyReply"))

        assertNotNull(component.replyAddresses, "Operation Reply Addresses should be parsed")
        assertTrue(component.replyAddresses.containsKey("MyReplyAddress"))

        assertNotNull(component.tags, "Tags should be parsed")
        assertTrue(component.tags.containsKey("MyTag"))

        assertNotNull(component.externalDocs, "External Docs should be parsed")
        assertTrue(component.externalDocs.containsKey("MyDocs"))

        assertNotNull(component.operationTraits, "Operation Traits should be parsed")
        assertTrue(component.operationTraits.containsKey("MyOpTrait"))

        assertNotNull(component.messageTraits, "Message Traits should be parsed")
        assertTrue(component.messageTraits.containsKey("MyMsgTrait"))

        assertNotNull(component.serverBindings, "Server Bindings should be parsed")
        assertTrue(component.serverBindings.containsKey("MyServerBinding"))

        assertNotNull(component.channelBindings, "Channel Bindings should be parsed")
        assertTrue(component.channelBindings.containsKey("MyChannelBinding"))

        assertNotNull(component.operationBindings, "Operation Bindings should be parsed")
        assertTrue(component.operationBindings.containsKey("MyOpBinding"))

        assertNotNull(component.messageBindings, "Message Bindings should be parsed")
        assertTrue(component.messageBindings.containsKey("MyMsgBinding"))
    }
}
