package dev.banking.asyncapi.generator.core.generator.output

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeneratedOutputManifestTest {
    @TempDir
    lateinit var tempDir: Path

    private val sources: File get() = tempDir.resolve("generated").toFile()
    private val manifestFile: File get() = tempDir.resolve("build/output-manifest.txt").toFile()

    @Test
    fun `removes files an execution no longer generates and keeps everything else`() {
        val unrelated = sources.resolve("com/example/HandWritten.kt").apply { parentFile.mkdirs(); writeText("keep") }
        write("models", "com/example/User.kt", "com/example/old/Legacy.kt")

        write("models", "com/example/User.kt")

        assertTrue(sources.resolve("com/example/User.kt").exists())
        assertFalse(sources.resolve("com/example/old/Legacy.kt").exists())
        assertFalse(sources.resolve("com/example/old").exists(), "an emptied package directory is removed")
        assertTrue(unrelated.exists())
    }

    @Test
    fun `keeps a file another execution still generates`() {
        write("first", "com/example/Shared.kt")
        write("second", "com/example/Shared.kt")

        write("first", "com/example/Other.kt")

        assertTrue(sources.resolve("com/example/Shared.kt").exists())

        write("second", "com/example/Other.kt")

        assertFalse(sources.resolve("com/example/Shared.kt").exists(), "removed once no execution generates it")
    }

    private fun write(owner: String, vararg relativePaths: String) {
        FileSystemGeneratedArtifactWriter(
            sourceOutputDirectory = sources,
            resourceOutputDirectory = sources,
            manifest = GeneratedOutputManifest(manifestFile, owner),
        ).write(
            GenerationResult(
                relativePaths.map { GeneratedArtifact(relativePath = it, content = "// $owner", kind = GeneratedArtifactKind.SOURCE) },
            ),
        )
    }
}
