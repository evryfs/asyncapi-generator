package dev.banking.asyncapi.generator.core.parser.parameters

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ParameterParserTest {

    private val context = AsyncApiContext()
    private val parser = ParameterParser(context)

    @Test
    fun `parses every parameter field`() {
        val file = TestResources.file("parser/channels/asyncapi_parser_channel_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val parametersNode = ParserNodeFactory.root(document, context)
            .expectObject().required("channels")
            .expectObject().required("lightStatus")
            .expectObject().required("parameters")

        val parameters = parser.parseMap(parametersNode)

        val city = assertIs<ParameterInterface.ParameterInline>(parameters["city"]).parameter
        assertEquals("The city where the streetlights are located.", city.description)
        assertEquals("\$message.payload#/city", city.location)
        assertEquals(listOf("helsinki", "oslo", "stockholm"), city.enum)
        assertEquals("helsinki", city.default)
        assertEquals(listOf("helsinki", "oslo"), city.examples)
    }
}
