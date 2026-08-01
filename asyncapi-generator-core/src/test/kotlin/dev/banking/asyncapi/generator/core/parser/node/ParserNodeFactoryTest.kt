package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentSource
import dev.banking.asyncapi.generator.core.fixtures.TestResources
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
        val file = TestResources.file("reader/yaml/source-map.yaml")
        val source = DocumentSource(
            id = "source-map",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )
        val document = reader.read(source)
        val root = ParserNodeFactory.root(document, context)

        assertEquals("source_map.root", root.name)
        assertEquals("source_map.root", root.path)
        assertEquals(document.root, root.node)
        assertEquals(context, root.context)
    }

    @Test
    fun `registers source locations using parser path convention`() {
        val context = AsyncApiContext()
        val file = TestResources.file("reader/yaml/source-map.yaml")
        val source = DocumentSource(
            id = "source-map",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )
        val document = reader.read(source)
        ParserNodeFactory.root(document, context)

        assertEquals(1, context.sourceRepository.getLine("source_map.root"))
        assertEquals(2, context.sourceRepository.getLine("source_map.root.info"))
        assertEquals(3, context.sourceRepository.getLine("source_map.root.info.title"))
        assertEquals(5, context.sourceRepository.getLine("source_map.root.info.tags[0]"))
        assertEquals(5, context.sourceRepository.getLine("source_map.root.info.tags.0"))
        assertEquals(file, context.findFileById("source_map"))
    }

    @Test
    fun `registers full source locations using parser path convention`() {
        val context = AsyncApiContext()
        val file = TestResources.file("reader/yaml/source-map.yaml")
        val source = DocumentSource(
            id = "source-map",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )
        val document = reader.read(source)
        ParserNodeFactory.root(document, context)

        val titleLocation = assertNotNull(
            context.sourceRepository.getLocation("source_map.root.info.title"),
        )
        assertEquals("source-map", titleLocation.sourceId)
        assertEquals(file, titleLocation.file)
        assertEquals("source_map.root.info.title", titleLocation.path)
        assertEquals(3, titleLocation.line)
        assertEquals(3, titleLocation.column)

        val normalizedArrayLocation = assertNotNull(
            context.sourceRepository.getLocation("source_map.root.info.tags.0"),
        )
        assertEquals("source_map.root.info.tags.0", normalizedArrayLocation.path)
        assertEquals(5, normalizedArrayLocation.line)
    }

    @Test
    fun `strict expectations report locations supplied by the document reader`() {
        val context = AsyncApiContext()
        val file = TestResources.file("reader/yaml/source-map.yaml")
        val source = DocumentSource(
            id = "source-map",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )
        val document = reader.read(source)
        val title = ParserNodeFactory.root(document, context)
            .required("info")
            .required("title")

        val failure = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            title.expect<Boolean>()
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(failure.diagnostic)

        assertEquals(ParserValueType.STRING, diagnostic.actualType)
        assertEquals("source_map.root.info.title", diagnostic.path)
        assertEquals(file, diagnostic.sourceLocation.file)
        assertEquals(3, diagnostic.sourceLocation.line)
        assertEquals(10, diagnostic.sourceLocation.column)
    }
}
