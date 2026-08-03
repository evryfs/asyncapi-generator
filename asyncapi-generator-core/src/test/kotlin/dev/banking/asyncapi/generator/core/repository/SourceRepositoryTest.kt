package dev.banking.asyncapi.generator.core.repository

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentSource
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SourceRepositoryTest {

    @Test
    fun `same named files retain distinct source owned addresses`() {
        val context = AsyncApiContext()
        val left = parserRoot(context, File("left/shared.yaml"), "name: left")
        val right = parserRoot(context, File("right/shared.yaml"), "name: right")

        assertNotEquals(left.address.sourceId, right.address.sourceId)
        assertNotEquals(left.address, right.address)
        assertEquals("left", left.expectObject().required("name").expect<String>())
        assertEquals("right", right.expectObject().required("name").expect<String>())
        assertEquals(
            left.address.member("name"),
            context.sourceRepository.resolveAddress(left.address.sourceId, listOf("name")),
        )
        assertEquals(
            right.address.member("name"),
            context.sourceRepository.resolveAddress(right.address.sourceId, listOf("name")),
        )
    }

    private fun parserRoot(
        context: AsyncApiContext,
        file: File,
        content: String,
    ) = ParserNodeFactory.root(
        DocumentReaderRegistry.read(
            DocumentSource(
                id = file.nameWithoutExtension,
                file = file.canonicalFile,
                content = content,
                format = DocumentFormat.YAML,
            ),
        ),
        context,
    )
}
