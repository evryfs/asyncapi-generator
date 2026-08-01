package dev.banking.asyncapi.generator.core.reader

import assertk.assertFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.messageContains
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

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

        assertFailure {
            limits.readContent(file)
        }.isInstanceOf<DocumentReadException.ResourceLimitExceeded>()
            .messageContains(file.absolutePath)
    }
}
