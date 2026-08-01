package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.ReaderFixtures
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.reader.YamlDocumentReader
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ParserNodeFactoryTest {

    private val reader = YamlDocumentReader()

    @Test
    fun `creates parser root node from input document`() {
        val context = AsyncApiContext()
        val document = reader.read(ReaderFixtures.yamlSource("source-map.yaml"))
        val root = ParserNodeFactory.root(document, context)
        assertEquals("source_map.root", root.name)
        assertEquals("source_map.root", root.path)
        assertEquals(document.root, root.node)
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

    @Test
    fun `strict expectations report locations supplied by the document reader`() {
        val context = AsyncApiContext()
        val document = reader.read(ReaderFixtures.yamlSource("source-map.yaml"))
        val title = ParserNodeFactory.root(document, context)
            .required("info")
            .required("title")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            title.expect<Boolean>()
        }

        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)
        assertEquals(ParserValueType.STRING, diagnostic.actualType)
        assertEquals("source_map.root.info.title", diagnostic.path)
        assertEquals(ReaderFixtures.yamlFile("source-map.yaml"), diagnostic.sourceLocation.file)
        assertEquals(3, diagnostic.sourceLocation.line)
        assertEquals(10, diagnostic.sourceLocation.column)
    }
}
