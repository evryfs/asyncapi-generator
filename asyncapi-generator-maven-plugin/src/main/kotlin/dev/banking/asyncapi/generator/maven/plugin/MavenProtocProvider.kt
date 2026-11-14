package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.core.generator.protobuf.ProtocPlatform
import dev.banking.asyncapi.generator.core.generator.protobuf.ProtocProvider
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.ArtifactRequest
import java.nio.file.Path

/**
 * Resolves `com.google.protobuf:protoc` for this platform through the project's Maven repositories,
 * so mirrors, proxies and credentials from `settings.xml` apply as for any other dependency.
 */
internal class MavenProtocProvider(
    private val repositorySystem: RepositorySystem,
    private val session: RepositorySystemSession,
    private val repositories: List<RemoteRepository>,
) : ProtocProvider {
    override fun protoc(version: String): Path {
        val artifact = DefaultArtifact("com.google.protobuf", "protoc", ProtocPlatform.classifier(), "exe", version)
        val file = repositorySystem.resolveArtifact(session, ArtifactRequest(artifact, repositories, null)).artifact.file
        file.setExecutable(true)
        return file.toPath()
    }
}
