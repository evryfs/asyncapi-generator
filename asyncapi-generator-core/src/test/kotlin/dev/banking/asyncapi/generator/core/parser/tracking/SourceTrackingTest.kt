package dev.banking.asyncapi.generator.core.parser.tracking

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.source.DocumentSource
import dev.banking.asyncapi.generator.core.source.InputFormat
import dev.banking.asyncapi.generator.core.source.SourceReader
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SourceTrackingTest {
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
            context.nodeLocations.resolveAddress(left.address.sourceId, listOf("name")),
        )
        assertEquals(
            right.address.member("name"),
            context.nodeLocations.resolveAddress(right.address.sourceId, listOf("name")),
        )
    }

    @Test
    fun `registering a file again keeps its id`() {
        val tracking = SourceTracking()
        val file = File("same/file.yaml")

        assertEquals(tracking.registerSource(file, "a"), tracking.registerSource(file, "b"))
    }

    private fun parserRoot(
        context: AsyncApiContext,
        file: File,
        content: String,
    ) = ParserNodeFactory.root(
        SourceReader.read(
            DocumentSource(
                file = file.canonicalFile,
                content = content,
                format = InputFormat.YAML,
            ),
        ),
        context,
    )
}
