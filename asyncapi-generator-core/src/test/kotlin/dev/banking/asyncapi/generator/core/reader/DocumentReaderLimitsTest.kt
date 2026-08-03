package dev.banking.asyncapi.generator.core.reader

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DocumentReaderLimitsTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `bounds file reads before loading the complete content`() {
        val file = tempDir.resolve("oversized.yaml").toFile()
        file.writeText("123456789")
        val limits =
            DocumentReaderLimits.DEFAULT.copy(
                maxDocumentBytes = 8,
                maxDocumentCharacters = 8,
            )

        val failure = assertFailsWith<DocumentReadException.ResourceLimitExceeded> {
            limits.readContent(file)
        }
        assertEquals(file, failure.file)
        assertEquals(DocumentResourceLimit.DOCUMENT_BYTES, failure.limit)
        assertEquals(8L, failure.maximum)
        assertTrue(failure.message.orEmpty().contains(file.absolutePath))
    }
}
