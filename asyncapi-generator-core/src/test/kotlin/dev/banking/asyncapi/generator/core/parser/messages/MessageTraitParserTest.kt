package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MessageTraitParserTest {

    private val context = AsyncApiContext()
    private val parser = MessageTraitParser(context)

    @Test
    fun `parses all message trait fields`() {
        val file = TestResources.file("parser/messages/asyncapi_parser_message_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val traitsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messageTraits")

        val traits = parser.parseMap(traitsNode)

        val trait = assertIs<MessageTraitInterface.InlineMessageTrait>(traits["commonHeaders"]).trait
        val headers = assertIs<SchemaInterface.SchemaInline>(trait.headers).schema
        assertIs<SchemaInterface.SchemaInline>(headers.properties?.get("myHeaders"))
        val correlationId = assertIs<CorrelationIdInterface.CorrelationIdInline>(trait.correlationId).correlationId
        assertEquals("\$message.header#/correlationId", correlationId.location)
        assertEquals("application/json", trait.contentType)
        assertEquals("commonHeaders", trait.name)
        assertEquals("Common message headers", trait.title)
        assertEquals("Shared message metadata", trait.summary)
        assertEquals("Applied to every streetlight message", trait.description)
        val tag = assertIs<TagInterface.TagInline>(trait.tags?.single()).tag
        assertEquals("shared", tag.name)
        val externalDocs = assertIs<ExternalDocInterface.ExternalDocInline>(trait.externalDocs).externalDoc
        assertEquals("https://example.com/docs/message-trait", externalDocs.url)
        val binding = assertIs<BindingInterface.BindingInline>(trait.bindings?.get("kafka")).binding
        assertEquals("string", assertIs<SchemaInterface.SchemaInline>(binding.kafkaKeySchema).schema.type)
        assertEquals("commonHeadersExample", trait.examples?.single()?.name)
    }

}
