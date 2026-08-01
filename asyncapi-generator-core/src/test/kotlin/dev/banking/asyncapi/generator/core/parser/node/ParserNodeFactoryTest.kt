package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.ReaderFixtures
import dev.banking.asyncapi.generator.core.reader.YamlDocumentReader
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class ParserNodeFactoryTest {

    private val reader = YamlDocumentReader()

    @Test
    fun `creates parser root node from input document`() {
        val context = AsyncApiContext()
        val document = reader.read(ReaderFixtures.yamlSource("source-map.yaml"))
        val root = ParserNodeFactory.root(document, context)
        assertEquals("source_map.root", root.name)
        assertEquals("source_map.root", root.path)
        assertSame(document.root, root.node)
        assertEquals(context, root.context)
    }

    @Test
    fun `registers source locations using parser path convention`() {
        val context = AsyncApiContext()
        val document = reader.read(ReaderFixtures.yamlSource("source-map.yaml"))
        ParserNodeFactory.root(document, context)
        assertEquals(1, context.sourceRepository.getLine("source_map.root"))
        assertEquals(2, context.sourceRepository.getLine("source_map.root.info"))
        assertEquals(3, context.sourceRepository.getLine("source_map.root.info.title"))
        assertEquals(5, context.sourceRepository.getLine("source_map.root.info.tags[0]"))
        assertEquals(5, context.sourceRepository.getLine("source_map.root.info.tags.0"))
        assertEquals(ReaderFixtures.yamlFile("source-map.yaml"), context.findFileById("source_map"))
    }

    @Test
    fun `registers full source locations using parser path convention`() {
        val context = AsyncApiContext()
        val document = reader.read(ReaderFixtures.yamlSource("source-map.yaml"))
        ParserNodeFactory.root(document, context)

        val titleLocation = assertNotNull(context.sourceRepository.getLocation("source_map.root.info.title"))
        assertEquals("source-map", titleLocation.sourceId)
        assertEquals(ReaderFixtures.yamlFile("source-map.yaml"), titleLocation.file)
        assertEquals("source_map.root.info.title", titleLocation.path)
        assertEquals(3, titleLocation.line)
        assertEquals(3, titleLocation.column)

        val normalizedArrayLocation = assertNotNull(context.sourceRepository.getLocation("source_map.root.info.tags.0"))
        assertEquals("source_map.root.info.tags.0", normalizedArrayLocation.path)
        assertEquals(5, normalizedArrayLocation.line)
    }
}
