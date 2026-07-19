package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.generator.AsyncApiGenerator
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationFactory
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationRequest
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class AsyncApiGeneratorMojo : AbstractMojo() {
    @Parameter(defaultValue = "\${project}", readonly = true)
    private lateinit var project: MavenProject

    @Parameter(property = "generatorName", defaultValue = "kotlin")
    private lateinit var generatorName: String

    @Parameter(property = "inputFile", required = true)
    private lateinit var inputFile: File

    @Parameter(property = "outputFile")
    private var outputFile: File? = null

    @Parameter(
        property = "codegenOutputDirectory",
        defaultValue = "\${project.build.directory}/generated-sources/asyncapi",
    )
    private lateinit var codegenOutputDirectory: File

    @Parameter(
        property = "resourceOutputDirectory",
        defaultValue = "\${project.build.directory}/generated-resources/asyncapi",
    )
    private lateinit var resourceOutputDirectory: File

    @Parameter(
        property = "javaSourceOutputDirectory",
        defaultValue = "\${project.build.directory}/generated-sources/asyncapi-java",
    )
    private var javaSourceOutputDirectory: File? = null

    @Parameter
    private var models: MavenModelGenerationConfiguration? = null

    @Parameter
    private var schemas: MavenSchemaGenerationConfiguration? = null

    @Parameter
    private var clients: MavenClientGenerationConfiguration? = null

    private val context = AsyncApiContext()
    private val parser = AsyncApiParser(context)
    private val validator = AsyncApiValidator(context)
    private val bundler = AsyncApiBundler()
    private val generator = AsyncApiGenerator()

    override fun execute() {
        try {
            log.info("asyncapi-generator-maven-plugin started")
            if (!inputFile.exists()) {
                throw MojoExecutionException("Input file not found: $inputFile")
            }
            val root = AsyncApiRegistry.readYaml(inputFile, context)
            val asyncApiParsed = parser.parse(root)
            val validationErrors = validator.validate(asyncApiParsed)
            validationErrors.logWarnings()
            validationErrors.throwErrors()
            val bundled = bundler.bundle(asyncApiParsed)
            outputFile?.let { file ->
                log.info("Writing bundled AsyncAPI specification to: ${file.absolutePath}")
                AsyncApiRegistry.writeYaml(file, bundled)
            }
            val targetLanguage =
                SourceLanguage.fromConfigurationValue(
                    value = generatorName,
                    path = "generatorName",
                )
            val modelRequest = models?.toRequest()
            val resolvedJavaSourceOutputDirectory =
                javaSourceOutputDirectory ?: defaultJavaSourceOutputDirectory()

            val generatorConfiguration =
                GeneratorConfigurationFactory.create(
                    GeneratorConfigurationRequest(
                        language = targetLanguage,
                        sourceOutputDirectory = codegenOutputDirectory,
                        javaSourceOutputDirectory = resolvedJavaSourceOutputDirectory,
                        resourceOutputDirectory = resourceOutputDirectory,
                        models = modelRequest,
                        schemas = schemas?.toRequest() ?: GeneratorConfigurationRequest.Schemas(),
                        clients = clients?.toRequest() ?: GeneratorConfigurationRequest.Clients(),
                    ),
                )
            if (generatorConfiguration.hasConfiguredOutputs()) {
                generator.generate(bundled, generatorConfiguration)
            }
            project.addCompileSourceRoot(codegenOutputDirectory.absolutePath)
            project.addCompileSourceRoot(resolvedJavaSourceOutputDirectory.absolutePath)
            log.info("asyncapi-generator-maven-plugin completed successfully")
        } catch (e: MojoExecutionException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw MojoExecutionException(e.message, e)
        } catch (e: Exception) {
            throw MojoExecutionException(e.message, e)
        }
    }

    private fun defaultJavaSourceOutputDirectory(): File =
        codegenOutputDirectory.parentFile?.resolve("asyncapi-java") ?: codegenOutputDirectory
}
