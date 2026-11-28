package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.core.GeneratorSettings
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.clientConfig
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.clientPackage
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.consumer
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.generatorName
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.inputPath
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.inputSpec
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.modelConfig
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.modelPackage
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.outputDirectory
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.outputFile
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.outputPath
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.producer
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.project
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.schemaPackage
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.validationAnnotations
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.project.MavenProject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

class AsyncApiGeneratorMojoTest {
    @Test
    fun `should generate kotlin models when model package is configured`() {
        AsyncApiGeneratorMojo()
            .apply {
                project(MavenProject())
                inputSpec(inputPath("asyncapi_valid_content_kotlin.yaml"))
                outputDirectory(outputPath("target/generated-sources/asyncapi"))
                modelPackage("com.example.model")
                generatorName("kotlin")
            }.execute()

        val output = File("target/generated-sources/asyncapi/com/example/model")
        assertTrue(output.exists(), "Output directory should exist")
        assertTrue(output.list()?.isNotEmpty() == true, "Output directory should not be empty")
    }

    @Test
    fun `should map every client bean field to the client settings`() {
        val settings =
            clientConfig(
                clientType = "spring-kafka",
                clientContract = "interface",
                producer =
                    producer(
                        enabled = false,
                        additionalPayloadTypes = listOf("byte-array", "string"),
                    ),
                consumer = consumer(enabled = true),
                topicParameterProperties = mapOf("environment" to "kafka.environment"),
                validationAnnotations =
                    validationAnnotations(
                        clientContract = "org.springframework.validation.annotation.Validated",
                        payloadParameter = "jakarta.validation.Valid",
                    ),
            ).toSettings()

        assertEquals(
            GeneratorSettings.ClientConfig(
                clientType = "spring-kafka",
                clientContract = "interface",
                producer =
                    GeneratorSettings.ProducerConfig(
                        enabled = false,
                        additionalPayloadTypes = listOf("byte-array", "string"),
                    ),
                consumer = GeneratorSettings.ConsumerConfig(enabled = true),
                topicParameterProperties = mapOf("environment" to "kafka.environment"),
                validationAnnotations =
                    GeneratorSettings.ValidationAnnotationsConfig(
                        clientContract = "org.springframework.validation.annotation.Validated",
                        payloadParameter = "jakarta.validation.Valid",
                    ),
            ),
            settings,
        )
    }
}
