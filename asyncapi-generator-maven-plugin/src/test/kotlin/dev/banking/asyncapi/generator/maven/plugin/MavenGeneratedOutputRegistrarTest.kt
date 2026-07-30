package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationFactory
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationRequest
import dev.banking.asyncapi.generator.core.generator.configuration.ModelType
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import org.apache.maven.project.MavenProject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class MavenGeneratedOutputRegistrarTest {
    @Test
    fun `registers generated source output as a compile source root`() {
        val project = MavenProject()
        val outputDirectory = outputDirectory("source")
        val configuration =
            configuration(
                generatorName = GeneratorName.KOTLIN,
                outputDirectory = outputDirectory,
                models =
                    GeneratorConfigurationRequest.Models(
                        packageName = "com.example.model",
                    ),
            )

        MavenGeneratedOutputRegistrar(project).register(configuration)

        assertEquals(
            listOf(outputDirectory.normalizedAbsolutePath()),
            project.compileSourceRoots,
        )
        assertTrue(project.resources.isEmpty())
    }

    @Test
    fun `registers schema output as a resource without generated source files`() {
        val project = MavenProject()
        val outputDirectory = outputDirectory("schema")
        val configuration =
            configuration(
                generatorName = GeneratorName.AVRO_SCHEMA,
                outputDirectory = outputDirectory,
                schemaPackageName = "com.example.schema",
            )

        MavenGeneratedOutputRegistrar(project).register(configuration)

        assertTrue(project.compileSourceRoots.isEmpty())
        assertEquals(1, project.resources.size)
        assertEquals(outputDirectory.normalizedAbsolutePath(), project.resources.single().directory)
        assertEquals(listOf("**/*.java", "**/*.kt"), project.resources.single().excludes)
    }

    @Test
    fun `registers separate Kotlin and Java roots for Protobuf Kotlin models`() {
        val project = MavenProject()
        val sourceOutputDirectory = outputDirectory("protobuf-kotlin")
        val javaSourceOutputDirectory = outputDirectory("protobuf-java")
        val resourceOutputDirectory = outputDirectory("protobuf-schema")
        val configuration =
            GeneratorConfigurationFactory.create(
                GeneratorConfigurationRequest(
                    generatorName = GeneratorName.KOTLIN,
                    sourceOutputDirectory = sourceOutputDirectory,
                    javaSourceOutputDirectory = javaSourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                    models =
                        GeneratorConfigurationRequest.Models(
                            packageName = "com.example.protobuf",
                            modelType = ModelType.PROTOBUF_MESSAGE,
                        ),
                ),
            )

        MavenGeneratedOutputRegistrar(project).register(configuration)

        assertEquals(
            setOf(
                sourceOutputDirectory.normalizedAbsolutePath(),
                javaSourceOutputDirectory.normalizedAbsolutePath(),
            ),
            project.compileSourceRoots.toSet(),
        )
        assertEquals(resourceOutputDirectory.normalizedAbsolutePath(), project.resources.single().directory)
    }

    @Test
    fun `registers Kotlin key models and Java native model roots for Avro Spring Kafka contracts`() {
        val project = MavenProject()
        val sourceOutputDirectory = outputDirectory("avro-kotlin-client")
        val javaSourceOutputDirectory = outputDirectory("avro-java-model")
        val resourceOutputDirectory = outputDirectory("avro-schema")
        val configuration =
            GeneratorConfigurationFactory.create(
                GeneratorConfigurationRequest(
                    generatorName = GeneratorName.KOTLIN,
                    sourceOutputDirectory = sourceOutputDirectory,
                    javaSourceOutputDirectory = javaSourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                    models =
                        GeneratorConfigurationRequest.Models(
                            packageName = "com.example.avro",
                            modelType = ModelType.AVRO_SPECIFIC_RECORD,
                        ),
                    clients =
                        GeneratorConfigurationRequest.Clients(
                            kafka =
                                GeneratorConfigurationRequest.Kafka(
                                    packageName = "com.example.client",
                                    modelPackageName = "com.example.avro",
                                    springKafka = GeneratorConfigurationRequest.KafkaSpringKafka(),
                                ),
                        ),
                ),
            )

        MavenGeneratedOutputRegistrar(project).register(configuration)

        assertEquals(
            setOf(
                sourceOutputDirectory.normalizedAbsolutePath(),
                javaSourceOutputDirectory.normalizedAbsolutePath(),
            ),
            project.compileSourceRoots.toSet(),
        )
        assertEquals(resourceOutputDirectory.normalizedAbsolutePath(), project.resources.single().directory)
    }

    @Test
    fun `does not register Maven roots for document output`() {
        val project = MavenProject()
        val outputDirectory = outputDirectory("document")
        val configuration =
            configuration(
                generatorName = GeneratorName.ASYNCAPI_JSON,
                outputDirectory = outputDirectory,
                outputFile = outputDirectory.resolve("asyncapi.json"),
            )

        MavenGeneratedOutputRegistrar(project).register(configuration)

        assertTrue(project.compileSourceRoots.isEmpty())
        assertTrue(project.resources.isEmpty())
    }

    @Test
    fun `does not register duplicate source or resource roots`() {
        val project = MavenProject()
        val outputDirectory = outputDirectory("native-avro")
        val configuration =
            configuration(
                generatorName = GeneratorName.JAVA,
                outputDirectory = outputDirectory,
                models =
                    GeneratorConfigurationRequest.Models(
                        packageName = "com.example.avro",
                        modelType = ModelType.AVRO_SPECIFIC_RECORD,
                    ),
            )
        val registrar = MavenGeneratedOutputRegistrar(project)

        registrar.register(configuration)
        registrar.register(configuration)

        assertEquals(1, project.compileSourceRoots.size)
        assertEquals(1, project.resources.size)
    }

    private fun configuration(
        generatorName: GeneratorName,
        outputDirectory: File,
        outputFile: File? = null,
        schemaPackageName: String? = null,
        models: GeneratorConfigurationRequest.Models? = null,
    ): GeneratorConfiguration =
        GeneratorConfigurationFactory.create(
            GeneratorConfigurationRequest(
                generatorName = generatorName,
                sourceOutputDirectory = outputDirectory,
                javaSourceOutputDirectory = outputDirectory,
                resourceOutputDirectory = outputDirectory,
                outputFile = outputFile,
                schemaPackageName = schemaPackageName,
                models = models,
            ),
        )

    private fun outputDirectory(name: String): File =
        File("target/generated-output-registration/$name")

    private fun File.normalizedAbsolutePath(): String =
        toPath().toAbsolutePath().normalize().toString()
}
