package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.core.generator.configuration.ClientContract
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
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
        AsyncApiGeneratorMojo().apply {
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
    fun `should map typed client contract and generation options`() {
        val clients =
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
            ).toRequest(
                clientPackage = "com.example.client",
                modelPackage = "com.example.model",
            )

        val kafka = clients.kafka
        assertNotNull(kafka)
        val springKafka = kafka!!.springKafka
        assertNotNull(springKafka)
        val configuredSpringKafka = springKafka!!
        assertEquals(ClientContract.INTERFACE, configuredSpringKafka.clientContract)
        assertFalse(configuredSpringKafka.producer.enabled)
        assertEquals(
            listOf("byte-array", "string"),
            configuredSpringKafka.producer.additionalPayloadTypes,
        )
        assertTrue(configuredSpringKafka.consumer.enabled)
        assertEquals(
            mapOf("environment" to "kafka.environment"),
            configuredSpringKafka.topicParameterProperties,
        )
        assertEquals(
            ClientValidationAnnotations(
                clientContract =
                    QualifiedTypeName.fromConfigurationValue(
                        value = "org.springframework.validation.annotation.Validated",
                        path = "clientConfig.validationAnnotations.clientContract",
                    ),
                payloadParameter =
                    QualifiedTypeName.fromConfigurationValue(
                        value = "jakarta.validation.Valid",
                        path = "clientConfig.validationAnnotations.payloadParameter",
                    ),
            ),
            configuredSpringKafka.validationAnnotations,
        )
    }

    @Test
    fun `should use producer and consumer defaults for minimal client config`() {
        val clients =
            clientConfig(
                clientType = "spring-kafka",
                clientContract = "interface",
            ).toRequest(
                clientPackage = "com.example.client",
                modelPackage = "com.example.model",
            )

        val springKafka = clients.kafka!!.springKafka!!
        assertTrue(springKafka.producer.enabled)
        assertTrue(springKafka.consumer.enabled)
        assertEquals(ClientValidationAnnotations(), springKafka.validationAnnotations)
    }

    @Test
    fun `should enable empty producer and consumer configuration objects`() {
        val clients =
            clientConfig(
                clientType = "spring-kafka",
                clientContract = "interface",
                producer = producer(),
                consumer = consumer(),
            ).toRequest(
                clientPackage = "com.example.client",
                modelPackage = "com.example.model",
            )

        val springKafka = clients.kafka!!.springKafka!!
        assertTrue(springKafka.producer.enabled)
        assertTrue(springKafka.consumer.enabled)
    }

    @Test
    fun `should require client type and client contract`() {
        val missingClientType =
            assertThrows<IllegalArgumentException> {
                clientConfig(clientContract = "interface").toRequest(
                    clientPackage = "com.example.client",
                    modelPackage = "com.example.model",
                )
            }
        val missingClientContract =
            assertThrows<IllegalArgumentException> {
                clientConfig(clientType = "spring-kafka").toRequest(
                    clientPackage = "com.example.client",
                    modelPackage = "com.example.model",
                )
            }

        assertEquals("clientConfig.clientType is required", missingClientType.message)
        assertEquals("clientConfig.clientContract is required", missingClientContract.message)
    }
}
