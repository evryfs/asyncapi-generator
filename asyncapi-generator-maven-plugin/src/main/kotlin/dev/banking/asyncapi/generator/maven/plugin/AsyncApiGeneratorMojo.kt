package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.generator.AsyncApiGenerator
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.generator.model.GeneratorOptions
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
import java.util.Locale

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

    @Parameter(property = "outputDir", defaultValue = "\${project.build.directory}/generated-sources/asyncapi")
    private lateinit var outputDir: File

    @Parameter(property = "modelPackage")
    private var modelPackage: String? = null

    @Parameter(property = "clientPackage")
    private var clientPackage: String? = null

    @Parameter(property = "schemaPackage")
    private var schemaPackage: String? = null

    @Parameter
    private var configOptions: Map<String, String> = emptyMap()

    private val context = AsyncApiContext()
    private val parser = AsyncApiParser(context)
    private val validator = AsyncApiValidator(context)
    private val bundler = AsyncApiBundler()
    private val generator = AsyncApiGenerator()

    override fun execute() {
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

        val targetLanguage = try {
            GeneratorName.valueOf(generatorName.uppercase(Locale.getDefault()))
        } catch (_: IllegalArgumentException) {
            throw MojoExecutionException(
                "Invalid generatorName '$generatorName'. Supported values: ${GeneratorName.entries.joinToString(", ")}"
            )
        }
        val clientType = configOptions["client.type"]
        val schemaType = configOptions["schema.type"]
        val modelNoArgAnnotation = configOptions["model.noArgAnnotation"]

        val hasModelPackage = modelPackage != null
        val hasClientPackage = clientPackage != null
        val hasSchemaPackage = schemaPackage != null

        if (clientType != null && !hasClientPackage) {
            throw MojoExecutionException("client.type requires clientPackage")
        }

        if (schemaType != null && !hasSchemaPackage) {
            throw MojoExecutionException("schema.type requires schemaPackage")
        }

        if (modelNoArgAnnotation != null && !hasModelPackage) {
            throw MojoExecutionException("model.noArgAnnotation requires modelPackage")
        }

        if (hasModelPackage || hasClientPackage || hasSchemaPackage) {

            val effectiveModelPackage = modelPackage ?: "unused"
            val effectiveClientPackage = clientPackage ?: "unused"
            val effectiveSchemaPackage = schemaPackage ?: "unused"

            val options = GeneratorOptions(
                generatorName = targetLanguage,
                modelPackage = effectiveModelPackage,
                clientPackage = effectiveClientPackage,
                schemaPackage = effectiveSchemaPackage,
                outputDir = outputDir,
                generateModels = hasModelPackage,
                generateSpringKafkaClient = hasClientPackage && clientType == "spring-kafka",
                generateQuarkusKafkaClient = hasClientPackage && clientType == "quarkus-kafka",
                generateAvroSchema = hasSchemaPackage && schemaType == "avro",
                configOptions = configOptions
            )
            generator.generate(bundled, options)
        }

        project.addCompileSourceRoot(outputDir.absolutePath)
        log.info("asyncapi-generator-maven-plugin completed successfully")
    }
}
