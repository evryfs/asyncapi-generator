package dev.banking.asyncapi.generator.core.parser.servers

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER_VARIABLE
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ServerVariableParserTest {

    private val context = AsyncApiContext()
    private val parser = ServerVariableParser(context)

    @Test
    fun `parse server variables`() {
        val file = TestResources.file("parser/servers/asyncapi_parser_servers_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val variablesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("servers")
            .expectObject().required("scram-connections")
            .expectObject().required("variables")

        val variables = parser.parseMap(variablesNode)

        val port = assertIs<ServerVariableInterface.ServerVariableInline>(variables["port"]).serverVariable
        assertEquals(listOf("18092", "28092"), port.enum)
        assertEquals("18092", port.default)
        assertEquals("The port used for Kafka connections", port.description)
        assertEquals(listOf("18092", "28092"), port.examples)
    }

    @Test
    fun `parses a referenced server variable with its concrete category`() {
        val file = TestResources.file("parser/servers/asyncapi_parser_servers_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val variablesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("serverVariables")

        val variables = parser.parseMap(variablesNode)

        val reference = assertIs<ServerVariableInterface.ServerVariableReference>(variables["referencedPort"])
            .reference
        assertEquals("#/components/serverVariables/sharedPort", reference.ref)
        assertEquals(SERVER_VARIABLE, reference.referenceCategoryKey)
    }
}
