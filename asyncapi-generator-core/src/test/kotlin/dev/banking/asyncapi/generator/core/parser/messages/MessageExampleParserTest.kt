package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MessageExampleParserTest {

    private val context = AsyncApiContext()
    private val parser = MessageExampleParser(context)

    @Test
    fun `parses every message example field`() {
        val file = TestResources.file("parser/messages/asyncapi_parser_message_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val examplesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messages")
            .expectObject().required("lightMeasured")
            .expectObject().required("examples")

        val examples = parser.parseList(examplesNode)

        assertEquals(2, examples.size)
        val example = examples.first()
        assertEquals("lightMeasurementExample", example.name)
        assertEquals("Example of light measurement payload", example.summary)
        assertNotNull(example.headers)
        assertEquals(
            mapOf(
                "lumens" to 1200,
                "sentAt" to "2024-09-12T12:00:00Z",
            ),
            example.payload,
        )
    }

}
