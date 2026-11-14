package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import dev.banking.asyncapi.generator.core.generator.AsyncApiGenerator
import dev.banking.asyncapi.generator.core.generator.output.GeneratedOutputManifest
import dev.banking.asyncapi.generator.core.generator.protobuf.MavenRepositoryProtocProvider
import dev.banking.asyncapi.generator.core.generator.protobuf.ProtocProvider
import dev.banking.asyncapi.generator.core.loader.AsyncApiDocumentLoader
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecution
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.repository.RemoteRepository
import java.io.File

@Mojo(
    name = "generate",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    threadSafe = true,
)
class AsyncApiGeneratorMojo : AbstractMojo() {
    @Parameter(defaultValue = "\${project}", readonly = true)
    private lateinit var project: MavenProject

    @Parameter(defaultValue = "\${mojoExecution}", readonly = true)
    private var mojoExecution: MojoExecution? = null

    @Component
    private var repositorySystem: RepositorySystem? = null

    @Parameter(defaultValue = "\${repositorySystemSession}", readonly = true)
    private var repositorySession: RepositorySystemSession? = null

    @Parameter(defaultValue = "\${project.remoteProjectRepositories}", readonly = true)
    private var remoteRepositories: List<RemoteRepository>? = null

    /**
     * A local `protoc` executable for native Protobuf models. By default `com.google.protobuf:protoc`
     * is resolved from the project's repositories.
     */
    @Parameter(property = "asyncapi.protocExecutable")
    private var protocExecutable: File? = null

    @Parameter(property = "generatorName", required = true)
    private var generatorName: String? = null

    @Parameter(property = "inputSpec", required = true)
    private lateinit var inputSpec: File

    @Parameter(property = "outputFile")
    private var outputFile: File? = null

    @Parameter(
        property = "outputDirectory",
        defaultValue = "\${project.build.directory}/generated-sources/asyncapi",
    )
    private lateinit var outputDirectory: File

    @Parameter(property = "modelPackage")
    private var modelPackage: String? = null

    @Parameter(property = "clientPackage")
    private var clientPackage: String? = null

    @Parameter(property = "schemaPackage")
    private var schemaPackage: String? = null

    @Parameter
    private var modelConfig: MavenModelConfiguration? = null

    @Parameter
    private var clientConfig: MavenClientConfiguration? = null

    /** Configuration warning ids to silence, for example `unused-client-config`. */
    @Parameter
    private var suppressedWarnings: List<String>? = null

    private val loader = AsyncApiDocumentLoader()
    private val bundler = AsyncApiBundler()

    override fun execute() {
        try {
            log.info("asyncapi-generator-maven-plugin started")
            loader.requireInputFile(inputSpec)
            val generatorConfiguration = generatorConfiguration()
            generatorConfiguration.warnings.forEach { warning -> log.warn(warning.message) }
            val loaded = loader.load(inputSpec)
            if (loaded.warnings.isNotEmpty()) {
                log.warn(loaded.formatWarnings().trimEnd())
            }
            val bundled = bundler.bundle(loaded)
            AsyncApiGenerator(protocProvider()).generate(bundled, generatorConfiguration)
            MavenGeneratedOutputRegistrar(project).register(generatorConfiguration)
            log.info("asyncapi-generator-maven-plugin completed successfully")
        } catch (e: MojoExecutionException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw MojoExecutionException(e.message, e)
        } catch (e: Exception) {
            throw MojoExecutionException(e.message, e)
        }
    }

    private fun generatorConfiguration() =
        MavenGeneratorConfigurationMapper.map(
            MavenGeneratorConfigurationRequest(
                generatorName = generatorName,
                outputDirectory = outputDirectory,
                outputFile = outputFile,
                modelPackage = modelPackage,
                clientPackage = clientPackage,
                schemaPackage = schemaPackage,
                modelConfig = modelConfig,
                clientConfig = clientConfig,
                suppressedWarnings = suppressedWarnings.orEmpty(),
                manifest = outputManifest(),
            ),
        )

    /** One manifest per project, shared by all executions, so executions writing to one directory cooperate. */
    private fun outputManifest(): GeneratedOutputManifest? =
        mojoExecution?.executionId?.let { executionId ->
            GeneratedOutputManifest(
                file = File(project.build.directory, "asyncapi-generator/output-manifest.txt"),
                owner = executionId,
            )
        }

    private fun protocProvider(): ProtocProvider {
        protocExecutable?.let { return MavenRepositoryProtocProvider(override = it.toPath()) }
        val system = repositorySystem
        val session = repositorySession
        return if (system != null && session != null) {
            MavenProtocProvider(system, session, remoteRepositories.orEmpty())
        } else {
            MavenRepositoryProtocProvider()
        }
    }
}
