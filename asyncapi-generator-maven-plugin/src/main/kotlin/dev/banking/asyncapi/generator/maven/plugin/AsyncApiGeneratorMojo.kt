package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.generator.AsyncApiGenerator
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationFactory
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationRequest
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
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
    private var schemaConfig: MavenSchemaConfiguration? = null

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
            if (!inputSpec.exists()) {
                throw MojoExecutionException("Input specification not found: $inputSpec")
            }
            val root = AsyncApiRegistry.read(inputSpec, context)
            val asyncApiParsed = parser.parse(root)
            val validationErrors = validator.validate(asyncApiParsed)
            validationErrors.logWarnings()
            validationErrors.throwErrors()
            val bundled = bundler.bundle(asyncApiParsed)
            outputFile?.let { file ->
                log.info("Writing bundled AsyncAPI specification to: ${file.absolutePath}")
                AsyncApiRegistry.writeYaml(file, bundled)
            }
            val targetGenerator =
                GeneratorName.fromConfigurationValue(
                    value = generatorName,
                    path = "generatorName",
                )
            val modelRequest =
                if (modelPackage != null || modelConfig != null) {
                    (modelConfig ?: MavenModelConfiguration()).toRequest(modelPackage)
                } else {
                    null
                }

            val generatorConfiguration =
                GeneratorConfigurationFactory.create(
                    GeneratorConfigurationRequest(
                        generatorName = targetGenerator,
                        sourceOutputDirectory = outputDirectory,
                        javaSourceOutputDirectory = outputDirectory,
                        resourceOutputDirectory = outputDirectory,
                        models = modelRequest,
                        schemas =
                            schemaConfig?.toRequest(schemaPackage)
                                ?: GeneratorConfigurationRequest.Schemas(),
                        clients =
                            clientConfig?.toRequest(
                                clientPackage = clientPackage,
                                modelPackage = modelPackage,
                            ) ?: GeneratorConfigurationRequest.Clients(),
                    ),
                )
            if (generatorConfiguration.hasConfiguredOutputs()) {
                generator.generate(bundled, generatorConfiguration)
            }
            project.addCompileSourceRoot(outputDirectory.absolutePath)
            log.info("asyncapi-generator-maven-plugin completed successfully")
        } catch (e: MojoExecutionException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw MojoExecutionException(e.message, e)
        } catch (e: Exception) {
            throw MojoExecutionException(e.message, e)
        }
    }
}
