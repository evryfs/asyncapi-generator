package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.fixtures.writeTestFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DocumentReaderLimitsTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `bounds file reads before loading the complete content`() {
        val file = tempDir.writeTestFile("oversized.yaml", "123456789")
        val limits =
            DocumentReaderLimits.DEFAULT.copy(
                maxDocumentBytes = 8,
                maxDocumentCharacters = 8,
            )

        val error = assertFailsWith<DocumentReadException.ResourceLimitExceeded> {
            limits.readContent(file)
        }

        assertTrue(error.message.orEmpty().contains(file.absolutePath))
    }
}
