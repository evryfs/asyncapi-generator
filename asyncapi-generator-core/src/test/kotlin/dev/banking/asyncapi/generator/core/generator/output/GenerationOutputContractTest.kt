package dev.banking.asyncapi.generator.core.generator.output

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerationOutputContractTest {
    @Test
    fun `generated artifact keeps relative path content and kind`() {
        val artifact =
            GeneratedArtifact(
                relativePath = "com/example/User.kt",
                content = "data class User(val id: String)",
                kind = GeneratedArtifactKind.SOURCE,
            )

        assertEquals("com/example/User.kt", artifact.relativePath)
        assertEquals("data class User(val id: String)", artifact.content)
        assertEquals(GeneratedArtifactKind.SOURCE, artifact.kind)
    }

    @Test
    fun `generated artifact rejects blank relative path`() {
        val failure =
            assertFailsWith<IllegalArgumentException> {
                GeneratedArtifact(
                    relativePath = " ",
                    content = "",
                    kind = GeneratedArtifactKind.SOURCE,
                )
            }

        assertEquals("Generated artifact relative path cannot be blank", failure.message)
    }

    @Test
    fun `generated artifact rejects absolute path`() {
        val failure =
            assertFailsWith<IllegalArgumentException> {
                GeneratedArtifact(
                    relativePath = "/tmp/generated/User.kt",
                    content = "",
                    kind = GeneratedArtifactKind.SOURCE,
                )
            }

        assertEquals("Generated artifact path must be relative: /tmp/generated/User.kt", failure.message)
    }

    @Test
    fun `generated artifact rejects parent directory segments`() {
        val failure =
            assertFailsWith<IllegalArgumentException> {
                GeneratedArtifact(
                    relativePath = "../outside/User.kt",
                    content = "",
                    kind = GeneratedArtifactKind.SOURCE,
                )
            }

        assertEquals("Generated artifact path cannot contain parent directory segments: ../outside/User.kt", failure.message)
    }

    @Test
    fun `generation result preserves artifact order`() {
        val first = artifact("com/example/User.kt", GeneratedArtifactKind.SOURCE)
        val second = artifact("com/example/schema/User.avsc", GeneratedArtifactKind.SCHEMA)

        val result = GenerationResult.of(first, second)

        assertEquals(listOf(first, second), result.artifacts)
    }

    @Test
    fun `generation result filters artifacts by kind`() {
        val source = artifact("com/example/User.kt", GeneratedArtifactKind.SOURCE)
        val schema = artifact("com/example/schema/User.avsc", GeneratedArtifactKind.SCHEMA)
        val resource = artifact("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports", GeneratedArtifactKind.RESOURCE)

        val result = GenerationResult.of(source, schema, resource)

        assertEquals(listOf(source), result.artifactsOfKind(GeneratedArtifactKind.SOURCE))
        assertEquals(listOf(schema), result.artifactsOfKind(GeneratedArtifactKind.SCHEMA))
        assertEquals(listOf(resource), result.artifactsOfKind(GeneratedArtifactKind.RESOURCE))
    }

    @Test
    fun `generation result combines artifacts`() {
        val source = artifact("com/example/User.kt", GeneratedArtifactKind.SOURCE)
        val schema = artifact("com/example/schema/User.avsc", GeneratedArtifactKind.SCHEMA)

        val result = GenerationResult.of(source) + GenerationResult.of(schema)

        assertEquals(listOf(source, schema), result.artifacts)
    }

    @Test
    fun `empty generation result contains no artifacts`() {
        val result = GenerationResult.Empty

        assertTrue(result.isEmpty())
        assertEquals(emptyList(), result.artifacts)
        assertFalse(GenerationResult.of(artifact("com/example/User.kt", GeneratedArtifactKind.SOURCE)).isEmpty())
    }

    private fun artifact(
        relativePath: String,
        kind: GeneratedArtifactKind,
    ): GeneratedArtifact =
        GeneratedArtifact(
            relativePath = relativePath,
            content = relativePath,
            kind = kind,
        )
}
