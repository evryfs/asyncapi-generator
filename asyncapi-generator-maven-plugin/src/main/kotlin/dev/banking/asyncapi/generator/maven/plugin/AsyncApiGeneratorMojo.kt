package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.generator.AsyncApiGenerator
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationReporter
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File

@Mojo(
    name = "generate",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    threadSafe = true,
)
class AsyncApiGeneratorMojo : AbstractMojo() {
    @Parameter(defaultValue = "\${project}", readonly = true)
    private lateinit var project: MavenProject

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

    private val context = AsyncApiContext()
    private val parser = AsyncApiParser(context)
    private val validator = AsyncApiValidator(context)
    private val bundler = AsyncApiBundler()
    private val generator = AsyncApiGenerator()

    override fun execute() {
        try {
            log.info("asyncapi-generator-maven-plugin started")
            val generatorConfiguration = generatorConfiguration()
            validateInputSpecification()
            val root = AsyncApiRegistry.read(inputSpec, context)
            val asyncApiParsed = parser.parse(root)
            val validationErrors = validator.validate(asyncApiParsed)
            ValidationReporter(context).logWarnings(validationErrors)
            ValidationReporter(context).throwErrors(validationErrors)
            val bundled = bundler.bundle(asyncApiParsed)
            generator.generate(bundled, generatorConfiguration)
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
            ),
        )

    private fun validateInputSpecification() {
        if (!inputSpec.exists()) {
            throw MojoExecutionException("Input specification not found: $inputSpec")
        }
        if (!inputSpec.isFile) {
            throw MojoExecutionException("Input specification must be a file: $inputSpec")
        }
        if (!inputSpec.canRead()) {
            throw MojoExecutionException("Input specification is not readable: $inputSpec")
        }
    }
}
