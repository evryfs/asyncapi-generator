package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OperationTraitParserTest {

    private val context = AsyncApiContext()
    private val parser = OperationTraitParser(context)

    @Test
    fun `parses all operation trait fields`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operations_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val traitsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("operationTraits")

        val traits = parser.parseMap(traitsNode)

        val kafka = assertIs<OperationTraitInterface.OperationTraitInline>(traits["kafka"]).operationTrait
        assertEquals("Kafka operation defaults", kafka.title)
        assertEquals("Shared Kafka operation settings", kafka.summary)
        assertEquals("Applied to Kafka operations", kafka.description)
        val security = assertIs<SecuritySchemeInterface.SecuritySchemeInline>(kafka.security?.single()).security
        assertEquals("userPassword", security.type)
        val tag = assertIs<TagInterface.TagInline>(kafka.tags?.single()).tag
        assertEquals("kafka", tag.name)
        val externalDocs = assertIs<ExternalDocInterface.ExternalDocInline>(kafka.externalDocs).externalDoc
        assertEquals("https://example.com/docs/operation-trait", externalDocs.url)
        val binding = assertIs<BindingInterface.BindingInline>(kafka.bindings?.get("kafka")).binding
        assertEquals(mapOf("groupId" to "streetlights"), binding.content)
        assertIs<OperationTraitInterface.OperationTraitInline>(traits["logging"])
    }
}
