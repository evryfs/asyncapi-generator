package dev.banking.asyncapi.generator.core.generator.output

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GeneratedArtifactPathsTest {
    @Test
    fun `from namespace converts dot-separated namespace to relative path`() {
        val path = GeneratedArtifactPaths.fromNamespace("com.example.model", "User.kt")

        assertEquals("com/example/model/User.kt", path)
    }

    @Test
    fun `from namespace returns file name when namespace is blank`() {
        val path = GeneratedArtifactPaths.fromNamespace(" ", "User.kt")

        assertEquals("User.kt", path)
    }

    @Test
    fun `from namespace rejects blank file name`() {
        val failure =
            assertFailsWith<IllegalArgumentException> {
                GeneratedArtifactPaths.fromNamespace("com.example.model", " ")
            }

        assertEquals("Generated artifact file name cannot be blank", failure.message)
    }
}
