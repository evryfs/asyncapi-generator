package dev.banking.asyncapi.generator.core.generator.protobuf

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProtocProviderTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `maps the platform to the classifiers protoc is published with`() {
        assertEquals("linux-x86_64", ProtocPlatform.classifier("Linux", "amd64"))
        assertEquals("osx-aarch_64", ProtocPlatform.classifier("Mac OS X", "aarch64"))
        assertEquals("windows-x86_64", ProtocPlatform.classifier("Windows 11", "x86_64"))
        assertFailsWith<UnsupportedOperationException> { ProtocPlatform.classifier("Plan 9", "amd64") }
    }

    @Test
    fun `uses protoc from the local Maven repository without downloading`() {
        val local = tempDir.resolve("com/google/protobuf/protoc/3.25.5/protoc-3.25.5-linux-x86_64.exe")
        Files.createDirectories(local.parent)
        Files.writeString(local, "#!/bin/sh\n")
        val provider =
            MavenRepositoryProtocProvider(
                localRepository = tempDir,
                remoteRepository = URI.create("http://127.0.0.1:9/unreachable/"),
                classifier = { "linux-x86_64" },
            )

        assertEquals(local, provider.protoc("3.25.5"))
    }

    @Test
    fun `an explicit executable wins and must exist`() {
        val explicit = Files.writeString(tempDir.resolve("protoc"), "#!/bin/sh\n")

        assertEquals(explicit, MavenRepositoryProtocProvider(override = explicit).protoc("3.25.5"))
        assertFailsWith<IllegalArgumentException> {
            MavenRepositoryProtocProvider(override = tempDir.resolve("missing")).protoc("3.25.5")
        }
    }
}
