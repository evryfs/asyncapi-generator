package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.clientPackage
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.configOptions
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.generatorName
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.inputPath
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.outputPath
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.inputFile
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.modelPackage
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.outputDir
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.outputFile
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.project
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.schemaPackage
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.project.MavenProject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

class AsyncApiGeneratorMojoTest {

    @Test
    fun `should generate kotlin models from valid asyncapi yaml`() {
        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputFile(inputPath("asyncapi_valid_content_kotlin.yaml"))
            outputDir(outputPath("target/generated-sources/asyncapi"))
            modelPackage("com.example.model")
            generatorName("kotlin")
        }.execute()
        val output = File("target/generated-sources/asyncapi/com/example/model")
        assertTrue(output.exists(), "Output directory should exist")
        assertTrue(output.list()?.isNotEmpty() == true, "Output directory should not be empty")
    }

    @Test
    fun `should generate kotlin kafka client from generic kafka yaml`() {
        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputFile(inputPath("asyncapi_kafka_complex.yaml"))
            outputDir(outputPath("target/generated-sources/asyncapi"))
            modelPackage("com.example.kafka.model")
            clientPackage("com.example.kafka.client")
            generatorName("kotlin")
            configOptions(mapOf(
                "client.type" to "spring-kafka"
            ))
        }.execute()
        val clientDir = File("target/generated-sources/asyncapi/com/example/kafka/client")
        assertTrue(clientDir.exists(), "Client directory should exist")
    }

    @Test
    fun `should generate java kafka client from generic kafka yaml`() {
        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputFile(inputPath("asyncapi_kafka_complex.yaml"))
            outputDir(outputPath("target/generated-sources/asyncapi"))
            modelPackage("com.example.kafka.model")
            clientPackage("com.example.kafka.client")
            generatorName("java")
            configOptions(mapOf(
                "client.type" to "spring-kafka"
            ))
        }.execute()
        val clientDir = File("target/generated-sources/asyncapi/com/example/kafka/client")
        assertTrue(clientDir.exists(), "Client directory should exist")
    }

    @Test
    fun `should support outputFile option to save bundled yaml`() {
        val bundledFile = File("target/generated-sources/asyncapi/bundled/asyncapi.bundled.yaml")
        if (bundledFile.exists()) bundledFile.delete()

        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputFile(inputPath("asyncapi_kafka_complex.yaml"))
            outputDir(outputPath("target/generated-sources/asyncapi"))
            outputFile(File("bundled/asyncapi.bundled.yaml"))
            modelPackage("com.example.bundled")
            generatorName("kotlin")
        }.execute()

        assertTrue(bundledFile.exists(), "Bundled output file should exist")
        assertTrue(bundledFile.length() > 0, "Bundled output file should not be empty")
    }

    @Test
    fun `should generate avro schema when schema type is avro`() {
        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputFile(inputPath("asyncapi_kafka_complex.yaml"))
            outputDir(outputPath("target/generated-sources/asyncapi"))
            modelPackage("com.example.avro.model")
            schemaPackage("com.example.avro.schema")
            generatorName("kotlin")
            configOptions(mapOf(
                "schema.type" to "avro"
            ))
        }.execute()
        val schemaDir = File("target/generated-sources/asyncapi/com/example/avro/schema")
        assertTrue(schemaDir.exists(), "Schema directory should exist")
    }

    @Test
    fun `should fail when input file is missing`() {
        val mojo = AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputFile(File("src/test/resources/non_existent.yaml"))
            outputDir(outputPath("target/should-fail"))
            modelPackage("com.fail")
        }
        assertThrows<MojoExecutionException> {
            mojo.execute()
        }
    }

    @Test
    fun `should fail when generator name is invalid`() {
        val mojo = AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputFile(inputPath("asyncapi_valid_content_kotlin.yaml"))
            outputDir(outputPath("target/should-fail-gen"))
            modelPackage("com.fail")
            generatorName("invalid-lang")
        }
        assertThrows<MojoExecutionException> {
            mojo.execute()
        }
    }
}
