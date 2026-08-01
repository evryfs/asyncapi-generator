package dev.banking.asyncapi.generator.core.parser.node

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.prop
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentSource
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.reader.YamlDocumentReader
import org.junit.jupiter.api.Test

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

        assertThat(root.name).isEqualTo("source_map.root")
        assertThat(root.path).isEqualTo("source_map.root")
        assertThat(root.node).isEqualTo(document.root)
        assertThat(root.context).isEqualTo(context)
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

        assertThat(context.sourceRepository.getLine("source_map.root")).isEqualTo(1)
        assertThat(context.sourceRepository.getLine("source_map.root.info")).isEqualTo(2)
        assertThat(context.sourceRepository.getLine("source_map.root.info.title")).isEqualTo(3)
        assertThat(context.sourceRepository.getLine("source_map.root.info.tags[0]")).isEqualTo(5)
        assertThat(context.sourceRepository.getLine("source_map.root.info.tags.0")).isEqualTo(5)
        assertThat(context.findFileById("source_map")).isEqualTo(file)
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

        val titleLocation = assertThat(
            context.sourceRepository.getLocation("source_map.root.info.title"),
        ).isNotNull()
        titleLocation.prop("sourceId") { it.sourceId }.isEqualTo("source-map")
        titleLocation.prop("file") { it.file }.isEqualTo(file)
        titleLocation.prop("path") { it.path }.isEqualTo("source_map.root.info.title")
        titleLocation.prop("line") { it.line }.isEqualTo(3)
        titleLocation.prop("column") { it.column }.isEqualTo(3)

        val normalizedArrayLocation = assertThat(
            context.sourceRepository.getLocation("source_map.root.info.tags.0"),
        ).isNotNull()
        normalizedArrayLocation.prop("path") { it.path }.isEqualTo("source_map.root.info.tags.0")
        normalizedArrayLocation.prop("line") { it.line }.isEqualTo(5)
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

        val diagnostic = assertFailure {
            title.expect<Boolean>()
        }.isInstanceOf<AsyncApiParseException.ParserDiagnosticFailure>()
            .prop("diagnostic") { it.diagnostic }
            .isInstanceOf<ParserDiagnostic.UnexpectedValueType>()

        diagnostic.prop("actualType") { it.actualType }.isEqualTo(ParserValueType.STRING)
        diagnostic.prop("path") { it.path }.isEqualTo("source_map.root.info.title")
        diagnostic.prop("source file") { it.sourceLocation.file }.isEqualTo(file)
        diagnostic.prop("source line") { it.sourceLocation.line }.isEqualTo(3)
        diagnostic.prop("source column") { it.sourceLocation.column }.isEqualTo(10)
    }
}
