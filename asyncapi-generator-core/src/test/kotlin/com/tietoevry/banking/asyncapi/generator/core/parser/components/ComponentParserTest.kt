package com.tietoevry.banking.asyncapi.generator.core.parser.components

import com.tietoevry.banking.asyncapi.generator.core.model.components.ComponentInterface
import com.tietoevry.banking.asyncapi.generator.core.parser.AbstractParserTest
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ComponentParserTest : AbstractParserTest() {

    private val parser = ComponentParser(asyncApiContext)

    @Test
    fun `parse components object delegates to all sub-parsers`() {
        val root = readYaml("src/test/resources/parser/components/asyncapi_parser_components_valid.yaml")
        val result = parser.parseElement(root.mandatory("components"))

        assertTrue(result is ComponentInterface.ComponentInline)
        val component = result.component

        assertNotNull(component.schemas, "Schemas should be parsed")
        assertTrue(component.schemas.containsKey("MySchema"))

        assertNotNull(component.servers, "Servers should be parsed")
        assertTrue(component.servers.containsKey("MyServer"))

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
