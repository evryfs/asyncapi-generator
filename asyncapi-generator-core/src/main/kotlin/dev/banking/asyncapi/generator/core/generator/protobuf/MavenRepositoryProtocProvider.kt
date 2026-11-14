package dev.banking.asyncapi.generator.core.generator.protobuf

import java.io.IOException
import java.net.ProxySelector
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Duration

/**
 * Finds `protoc` in a local Maven repository, downloading it from a remote repository first if needed.
 *
 * Downloads are checked against the repository's SHA-1 checksum and stored in the local
 * repository layout, so later runs and Maven builds reuse them.
 *
 * @param override An explicit `protoc` executable to use instead, for offline or locked-down machines.
 */
class MavenRepositoryProtocProvider(
    private val localRepository: Path = defaultLocalRepository(),
    private val remoteRepository: URI = MAVEN_CENTRAL,
    private val override: Path? = null,
    private val classifier: () -> String = { ProtocPlatform.classifier() },
) : ProtocProvider {

    override fun protoc(version: String): Path {
        override?.let { path ->
            require(Files.isRegularFile(path)) { "protoc executable does not exist: $path" }
            return path
        }
        val fileName = "protoc-$version-${classifier()}.exe"
        val relativePath = "com/google/protobuf/protoc/$version/$fileName"
        val local = localRepository.resolve(relativePath)
        if (!Files.isRegularFile(local)) download(relativePath, local)
        local.toFile().setExecutable(true)
        return local
    }

    private fun download(relativePath: String, destination: Path) {
        val base = remoteRepository.toString().trimEnd('/')
        val content = get("$base/$relativePath")
        val expectedSha1 = String(get("$base/$relativePath.sha1")).trim().substringBefore(' ')
        val actualSha1 = MessageDigest.getInstance("SHA-1").digest(content).joinToString("") { "%02x".format(it) }
        if (!actualSha1.equals(expectedSha1, ignoreCase = true)) {
            throw IOException("Checksum mismatch for $base/$relativePath: expected $expectedSha1, got $actualSha1")
        }
        Files.createDirectories(destination.parent)
        val temporary = Files.createTempFile(destination.parent, ".protoc-", ".tmp")
        try {
            Files.write(temporary, content)
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun get(url: String): ByteArray {
        val response =
            client.send(HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).GET().build(), HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() != 200) {
            throw IOException(
                "Could not download $url (HTTP ${response.statusCode()}). Without network access, " +
                    "point the generator at a local protoc executable.",
            )
        }
        return response.body()
    }

    companion object {
        val MAVEN_CENTRAL: URI = URI.create("https://repo.maven.apache.org/maven2/")
        private val TIMEOUT: Duration = Duration.ofMinutes(2)
        private val client: HttpClient =
            HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .proxy(ProxySelector.getDefault())
                .connectTimeout(Duration.ofSeconds(30))
                .build()

        fun defaultLocalRepository(): Path =
            System.getProperty("maven.repo.local")?.let(Path::of)
                ?: Path.of(System.getProperty("user.home"), ".m2", "repository")
    }
}
