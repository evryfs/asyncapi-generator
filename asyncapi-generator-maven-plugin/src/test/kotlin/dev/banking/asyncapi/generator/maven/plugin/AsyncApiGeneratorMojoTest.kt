package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.avroProjection
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.clientConfig
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.clientPackage
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.generatorName
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.inputPath
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.inputSpec
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.kafka
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.modelConfig
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.modelPackage
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.nativeAvro
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.nativeProtobuf
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.outputDirectory
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.outputFile
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.outputPath
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.project
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.schemaConfig
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.schemaPackage
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.project.MavenProject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
    fun `should generate kotlin kafka client from top-level packages`() {
        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputSpec(inputPath("asyncapi_kafka_complex.yaml"))
            outputDirectory(outputPath("target/generated-sources/asyncapi"))
            modelPackage("com.example.kafka.model")
            clientPackage("com.example.kafka.client")
            clientConfig(clientConfig(kafka = kafka()))
            generatorName("kotlin")
        }.execute()

        val clientDir = File("target/generated-sources/asyncapi/com/example/kafka/client")
        assertTrue(clientDir.exists(), "Client directory should exist")
    }

    @Test
    fun `should generate java kafka client from top-level packages`() {
        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputSpec(inputPath("asyncapi_kafka_complex.yaml"))
            outputDirectory(outputPath("target/generated-sources/asyncapi"))
            modelPackage("com.example.kafka.model")
            clientPackage("com.example.kafka.client")
            clientConfig(clientConfig(kafka = kafka()))
            generatorName("java")
        }.execute()

        val clientDir = File("target/generated-sources/asyncapi/com/example/kafka/client")
        assertTrue(clientDir.exists(), "Client directory should exist")
    }

    @Test
    fun `should accept java record model type from model config`() {
        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputSpec(inputPath("asyncapi_kafka_complex.yaml"))
            outputDirectory(outputPath("target/generated-sources/asyncapi"))
            modelPackage("com.example.record.model")
            modelConfig(modelConfig(javaModelType = "record"))
            generatorName("java")
        }.execute()

        val generatedRecord =
            File("target/generated-sources/asyncapi/com/example/record/model/User.java")
        assertTrue(generatedRecord.exists(), "Generated Java record should exist")
        assertTrue(generatedRecord.readText().contains("public record User("))
    }

    @Test
    fun `should support output file for bundled specification`() {
        val bundledFile = File("target/generated-sources/asyncapi/bundled/asyncapi.bundled.yaml")
        if (bundledFile.exists()) bundledFile.delete()

        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputSpec(inputPath("asyncapi_kafka_complex.yaml"))
            outputDirectory(outputPath("target/generated-sources/asyncapi"))
            outputFile(bundledFile)
            modelPackage("com.example.bundled")
            generatorName("kotlin")
        }.execute()

        assertTrue(bundledFile.exists(), "Bundled output file should exist")
        assertTrue(bundledFile.length() > 0, "Bundled output file should not be empty")
    }

    @Test
    fun `should generate avro projection under the shared output directory`() {
        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputSpec(inputPath("asyncapi_kafka_complex.yaml"))
            outputDirectory(outputPath("target/generated-sources/asyncapi"))
            modelPackage("com.example.avro.model")
            schemaPackage("com.example.avro.schema")
            schemaConfig(schemaConfig(avroProjection = avroProjection()))
            generatorName("kotlin")
        }.execute()

        val schemaDir = File("target/generated-sources/asyncapi/com/example/avro/schema")
        assertTrue(schemaDir.exists(), "Schema directory should exist")
    }

    @Test
    fun `should generate native avro schema and specific record in shared output directory`() {
        val outputDirectory = outputPath("target/generated-sources/asyncapi-native-avro")

        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputSpec(inputPath("asyncapi_native_avro.yaml"))
            outputDirectory(outputDirectory)
            schemaConfig(schemaConfig(nativeAvro = nativeAvro(enabled = true)))
            generatorName("avro")
        }.execute()

        val schemaFile = outputDirectory.resolve("com/example/avro/UserCreated.avsc")
        val specificRecordFile = outputDirectory.resolve("com/example/avro/UserCreated.java")
        assertTrue(schemaFile.exists(), "Native Avro schema output should exist")
        assertTrue(specificRecordFile.exists(), "SpecificRecord source output should exist")
        assertTrue(specificRecordFile.readText().contains("extends org.apache.avro.specific.SpecificRecordBase"))
    }

    @Test
    fun `should generate native protobuf schema and Java messages by default`() {
        val outputDirectory = outputPath("target/generated-sources/asyncapi-native-protobuf")

        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputSpec(inputPath("asyncapi_native_protobuf.yaml"))
            outputDirectory(outputDirectory)
            modelPackage("com.example.protobuf")
            generatorName("protobuf")
        }.execute()

        val schemaFile = outputDirectory.resolve("com/example/protobuf/UserCreated.proto")
        val javaMessageFile = outputDirectory.resolve("com/example/protobuf/UserCreated.java")
        assertTrue(schemaFile.exists(), "Native Protobuf schema output should exist")
        assertTrue(schemaFile.readText().contains("message UserCreated"))
        assertTrue(javaMessageFile.exists(), "Native Protobuf Java message output should exist")
        assertTrue(javaMessageFile.readText().contains("public final class UserCreated"))
    }

    @Test
    fun `should generate native protobuf schema without model package`() {
        val outputDirectory = outputPath("target/generated-sources/asyncapi-native-protobuf-schema-only")
        outputDirectory.deleteRecursively()
        outputDirectory.mkdirs()

        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputSpec(inputPath("asyncapi_native_protobuf.yaml"))
            outputDirectory(outputDirectory)
            schemaConfig(schemaConfig(nativeProtobuf = nativeProtobuf(enabled = true)))
            generatorName("protobuf")
        }.execute()

        val schemaFile = outputDirectory.resolve("com/example/protobuf/UserCreated.proto")
        val javaMessageFile = outputDirectory.resolve("com/example/protobuf/UserCreated.java")
        assertTrue(schemaFile.exists(), "Native Protobuf schema output should exist")
        assertFalse(javaMessageFile.exists(), "Native Protobuf Java message output should not exist")
    }

    @Test
    fun `should generate native protobuf Java messages and Kotlin DSL`() {
        val outputDirectory = outputPath("target/generated-sources/asyncapi-native-protobuf-kotlin")

        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputSpec(inputPath("asyncapi_native_protobuf.yaml"))
            outputDirectory(outputDirectory)
            modelPackage("com.example.protobuf")
            modelConfig(modelConfig(protobufModelType = "kotlin"))
            generatorName("protobuf")
        }.execute()

        assertTrue(outputDirectory.resolve("com/example/protobuf/UserCreated.java").exists())
        assertTrue(outputDirectory.resolve("com/example/protobuf/UserCreatedKt.kt").exists())
    }

    @Test
    fun `should allow bundle-only output with no packages`() {
        val bundledFile = File("target/generated-sources/asyncapi/bundled/asyncapi.bundle-only.yaml")
        if (bundledFile.exists()) bundledFile.delete()
        val bundleOnlyOutputDir = outputPath("target/generated-sources/asyncapi-bundle-only")

        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputSpec(inputPath("asyncapi_kafka_complex.yaml"))
            outputDirectory(bundleOnlyOutputDir)
            outputFile(bundledFile)
            generatorName("kotlin")
        }.execute()

        assertTrue(bundledFile.exists(), "Bundled output file should exist")
        assertTrue(bundledFile.length() > 0, "Bundled output file should not be empty")
        assertFalse(
            bundleOnlyOutputDir.resolve("com").exists(),
            "No code should be generated when packages are not set",
        )
    }

    @Test
    fun `should fail when input specification is missing`() {
        val mojo =
            AsyncApiGeneratorMojo().apply {
                project(MavenProject())
                inputSpec(File("src/test/resources/non_existent.yaml"))
                outputDirectory(outputPath("target/should-fail"))
                modelPackage("com.fail")
            }

        val exception = assertThrows<MojoExecutionException> { mojo.execute() }
        assertTrue(exception.message.orEmpty().startsWith("Input specification not found:"))
    }

    @Test
    fun `should fail when generator name is invalid`() {
        val mojo =
            AsyncApiGeneratorMojo().apply {
                project(MavenProject())
                inputSpec(inputPath("asyncapi_valid_content_kotlin.yaml"))
                outputDirectory(outputPath("target/should-fail-gen"))
                modelPackage("com.fail")
                generatorName("invalid-lang")
            }
        val exception = assertThrows<MojoExecutionException> { mojo.execute() }

        assertEquals(
            "Invalid generatorName 'invalid-lang'. Supported values: java, kotlin, avro, protobuf",
            exception.message,
        )
    }

    @Test
    fun `should fail when java model type is invalid`() {
        val mojo =
            AsyncApiGeneratorMojo().apply {
                project(MavenProject())
                inputSpec(inputPath("asyncapi_valid_content_kotlin.yaml"))
                outputDirectory(outputPath("target/should-fail-java-model-type"))
                modelPackage("com.fail")
                modelConfig(modelConfig(javaModelType = "data"))
                generatorName("java")
            }
        val exception = assertThrows<MojoExecutionException> { mojo.execute() }

        assertEquals(
            "Invalid models.javaModelType 'data'. Supported values: class, record",
            exception.message,
        )
    }
}
