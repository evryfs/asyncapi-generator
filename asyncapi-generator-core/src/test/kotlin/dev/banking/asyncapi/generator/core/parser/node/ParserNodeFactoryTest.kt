package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentSource
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ParserNodeFactoryTest {

    @Test
    fun `creates parser root node from input document`() {
        val context = AsyncApiContext()
        val file = File("source-map.yaml").canonicalFile
        val source = DocumentSource(
            id = "source-map",
            file = file,
            content =
                """
                asyncapi: '3.0.0'
                info:
                  title: Demo
                  tags:
                    - public
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )
        val document = DocumentReaderRegistry.read(source)

        val root = ParserNodeFactory.root(document, context)

        assertEquals("source_map.root", root.name)
        assertEquals("source_map.root", root.path)
        assertEquals(document.root, root.node)
        assertEquals(context, root.context)
    }

    @Test
    fun `registers source locations using parser path convention`() {
        val context = AsyncApiContext()
        val file = File("source-map.yaml").canonicalFile
        val source = DocumentSource(
            id = "source-map",
            file = file,
            content =
                """
                asyncapi: '3.0.0'
                info:
                  title: Demo
                  tags:
                    - public
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )
        val document = DocumentReaderRegistry.read(source)

        ParserNodeFactory.root(document, context)

        assertEquals(1, context.sourceRepository.getLine("source_map.root"))
        assertEquals(2, context.sourceRepository.getLine("source_map.root.info"))
        assertEquals(3, context.sourceRepository.getLine("source_map.root.info.title"))
        assertEquals(5, context.sourceRepository.getLine("source_map.root.info.tags[0]"))
        assertEquals(file, context.findFileById("source_map"))
    }

    @Test
    fun `registers full source locations using parser path convention`() {
        val context = AsyncApiContext()
        val file = File("source-map.yaml").canonicalFile
        val source = DocumentSource(
            id = "source-map",
            file = file,
            content =
                """
                asyncapi: '3.0.0'
                info:
                  title: Demo
                  tags:
                    - public
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )
        val document = DocumentReaderRegistry.read(source)

        ParserNodeFactory.root(document, context)

        val titleLocation = assertNotNull(
            context.sourceRepository.getLocation("source_map.root.info.title"),
        )
        assertEquals("source-map", titleLocation.sourceId)
        assertEquals(file, titleLocation.file)
        assertEquals("source_map.root.info.title", titleLocation.path)
        assertEquals(3, titleLocation.line)
        assertEquals(3, titleLocation.column)

        val arrayLocation = assertNotNull(
            context.sourceRepository.getLocation("source_map.root.info.tags[0]"),
        )
        assertEquals("source_map.root.info.tags[0]", arrayLocation.path)
        assertEquals(5, arrayLocation.line)
    }

    @Test
    fun `keeps complex members and array indexes collision safe`() {
        val context = AsyncApiContext()
        val source = DocumentSource(
            id = "identity",
            file = File("identity.yaml").canonicalFile,
            content =
                """
                "A.properties.x": dotted
                "bracket[name]": bracketed
                "0": numeric member
                items:
                  - numeric index
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )
        val root = ParserNodeFactory.root(DocumentReaderRegistry.read(source), context)
        val rootObject = root.expectObject()
        val dotted = rootObject.required("A.properties.x")
        val bracketed = rootObject.required("bracket[name]")
        val numericMember = rootObject.required("0")
        val numericIndex = rootObject.required("items").expectArray().elements().single()

        assertEquals("identity.root[\"A.properties.x\"]", dotted.path)
        assertEquals("identity.root[\"bracket[name]\"]", bracketed.path)
        assertEquals("identity.root.0", numericMember.path)
        assertEquals("identity.root.items[0]", numericIndex.path)
        assertEquals(
            numericMember.address,
            context.sourceRepository.resolveAddress(root.address.sourceId, listOf("0")),
        )
        assertEquals(
            numericIndex.address,
            context.sourceRepository.resolveAddress(root.address.sourceId, listOf("items", "0")),
        )
        assertNotNull(context.sourceRepository.getLocation(dotted.address))
        assertNotNull(context.sourceRepository.getLocation(bracketed.address))
    }

    @Test
    fun `strict expectations report locations supplied by the document reader`() {
        val context = AsyncApiContext()
        val file = File("source-map.yaml").canonicalFile
        val source = DocumentSource(
            id = "source-map",
            file = file,
            content =
                """
                asyncapi: '3.0.0'
                info:
                  title: Demo
                  tags:
                    - public
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )
        val document = DocumentReaderRegistry.read(source)
        val title = ParserNodeFactory.root(document, context)
            .expectObject().required("info")
            .expectObject().required("title")

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
