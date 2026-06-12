package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.avroProjection
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.clients
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.codegenOutputDirectory
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.generatorName
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.inputPath
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.javaSourceOutputDirectory
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.nativeAvro
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.nativeProtobuf
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.outputPath
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.inputFile
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.models
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.outputFile
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.project
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.resourceOutputDirectory
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.schemas
import dev.banking.asyncapi.generator.maven.plugin.MavenTestHelper.springKafka
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.project.MavenProject
import org.junit.jupiter.api.Assertions.assertEquals
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
            codegenOutputDirectory(outputPath("target/generated-sources/asyncapi"))
            resourceOutputDirectory(outputPath("target/generated-resources/asyncapi"))
            models(models(packageName = "com.example.model"))
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
            codegenOutputDirectory(outputPath("target/generated-sources/asyncapi"))
            resourceOutputDirectory(outputPath("target/generated-resources/asyncapi"))
            models(models(packageName = "com.example.kafka.model"))
            clients(clients(springKafka = springKafka(packageName = "com.example.kafka.client")))
            generatorName("kotlin")
        }.execute()
        val clientDir = File("target/generated-sources/asyncapi/com/example/kafka/client")
        assertTrue(clientDir.exists(), "Client directory should exist")
    }

    @Test
    fun `should generate java kafka client from generic kafka yaml`() {
        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputFile(inputPath("asyncapi_kafka_complex.yaml"))
            codegenOutputDirectory(outputPath("target/generated-sources/asyncapi"))
            resourceOutputDirectory(outputPath("target/generated-resources/asyncapi"))
            models(models(packageName = "com.example.kafka.model"))
            clients(clients(springKafka = springKafka(packageName = "com.example.kafka.client")))
            generatorName("java")
        }.execute()
        val clientDir = File("target/generated-sources/asyncapi/com/example/kafka/client")
        assertTrue(clientDir.exists(), "Client directory should exist")
    }

    @Test
    fun `should accept java record model type for java model generation`() {
        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputFile(inputPath("asyncapi_kafka_complex.yaml"))
            codegenOutputDirectory(outputPath("target/generated-sources/asyncapi"))
            resourceOutputDirectory(outputPath("target/generated-resources/asyncapi"))
            models(models(packageName = "com.example.record.model", javaModelType = "record"))
            generatorName("java")
        }.execute()

        val modelDir = File("target/generated-sources/asyncapi/com/example/record/model")
        assertTrue(modelDir.exists(), "Model directory should exist")
        val generatedRecord = modelDir.resolve("User.java")
        assertTrue(generatedRecord.readText().contains("public record User("))
    }

    @Test
    fun `should generate kafka client with explicit model package when models are not generated`() {
        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputFile(inputPath("asyncapi_kafka_complex.yaml"))
            codegenOutputDirectory(outputPath("target/generated-sources/asyncapi-client-only"))
            resourceOutputDirectory(outputPath("target/generated-resources/asyncapi-client-only"))
            clients(
                clients(
                    springKafka =
                        springKafka(
                            packageName = "com.example.kafka.client",
                            modelPackageName = "com.example.kafka.model",
                        ),
                ),
            )
            generatorName("kotlin")
        }.execute()

        val clientDir = File("target/generated-sources/asyncapi-client-only/com/example/kafka/client")
        val modelDir = File("target/generated-sources/asyncapi-client-only/com/example/kafka/model")
        assertTrue(clientDir.exists(), "Client directory should exist")
        assertTrue(!modelDir.exists(), "Model directory should not exist when model generation is not configured")
    }

    @Test
    fun `should support outputFile option to save bundled yaml`() {
        val bundledFile = File("target/generated-sources/asyncapi/bundled/asyncapi.bundled.yaml")
        if (bundledFile.exists()) bundledFile.delete()

        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputFile(inputPath("asyncapi_kafka_complex.yaml"))
            codegenOutputDirectory(outputPath("target/generated-sources/asyncapi"))
            resourceOutputDirectory(outputPath("target/generated-resources/asyncapi"))
            outputFile(File("target/generated-sources/asyncapi/bundled/asyncapi.bundled.yaml"))
            models(models(packageName = "com.example.bundled"))
            generatorName("kotlin")
        }.execute()

        assertTrue(bundledFile.exists(), "Bundled output file should exist")
        assertTrue(bundledFile.length() > 0, "Bundled output file should not be empty")
    }

    @Test
    fun `should generate avro schema when schema mode is avro projection`() {
        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputFile(inputPath("asyncapi_kafka_complex.yaml"))
            codegenOutputDirectory(outputPath("target/generated-sources/asyncapi"))
            resourceOutputDirectory(outputPath("target/generated-resources/asyncapi"))
            models(models(packageName = "com.example.avro.model"))
            schemas(schemas(avroProjection = avroProjection(packageName = "com.example.avro.schema")))
            generatorName("kotlin")
        }.execute()
        val schemaDir = File("target/generated-resources/asyncapi/com/example/avro/schema")
        assertTrue(schemaDir.exists(), "Schema directory should exist")
    }

    @Test
    fun `should generate native avro schema and specific record source`() {
        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputFile(inputPath("asyncapi_native_avro.yaml"))
            codegenOutputDirectory(outputPath("target/generated-sources/asyncapi-native-avro"))
            javaSourceOutputDirectory(outputPath("target/generated-sources/asyncapi-native-avro-java"))
            resourceOutputDirectory(outputPath("target/generated-resources/asyncapi-native-avro"))
            schemas(schemas(nativeAvro = nativeAvro(enabled = true)))
            generatorName("kotlin")
        }.execute()

        val schemaFile = File("target/generated-resources/asyncapi-native-avro/com/example/avro/UserCreated.avsc")
        val specificRecordFile = File("target/generated-sources/asyncapi-native-avro-java/com/example/avro/UserCreated.java")
        assertTrue(schemaFile.exists(), "Native Avro schema output should exist")
        assertTrue(specificRecordFile.exists(), "SpecificRecord source output should exist")
        assertTrue(specificRecordFile.readText().contains("extends org.apache.avro.specific.SpecificRecordBase"))
    }

    @Test
    fun `should generate native protobuf schema`() {
        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputFile(inputPath("asyncapi_native_protobuf.yaml"))
            codegenOutputDirectory(outputPath("target/generated-sources/asyncapi-native-protobuf"))
            resourceOutputDirectory(outputPath("target/generated-resources/asyncapi-native-protobuf"))
            schemas(schemas(nativeProtobuf = nativeProtobuf(enabled = true)))
            generatorName("kotlin")
        }.execute()

        val schemaFile = File("target/generated-resources/asyncapi-native-protobuf/com/example/protobuf/UserCreated.proto")
        assertTrue(schemaFile.exists(), "Native Protobuf schema output should exist")
        assertTrue(schemaFile.readText().contains("message UserCreated"))
    }

    @Test
    fun `should allow bundle-only output with no packages`() {
        val bundledFile = File("target/generated-sources/asyncapi/bundled/asyncapi.bundle-only.yaml")
        if (bundledFile.exists()) bundledFile.delete()
        val bundleOnlyOutputDir = outputPath("target/generated-sources/asyncapi-bundle-only")
        AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputFile(inputPath("asyncapi_kafka_complex.yaml"))
            codegenOutputDirectory(bundleOnlyOutputDir)
            resourceOutputDirectory(outputPath("target/generated-resources/asyncapi"))
            outputFile(File("target/generated-sources/asyncapi/bundled/asyncapi.bundle-only.yaml"))
            generatorName("kotlin")
            // no models/clients/schemas output blocks set
        }.execute()
        assertTrue(bundledFile.exists(), "Bundled output file should exist")
        assertTrue(bundledFile.length() > 0, "Bundled output file should not be empty")
        val generatedPackageRoot = bundleOnlyOutputDir.resolve("com")
        assertTrue(!generatedPackageRoot.exists(), "No code should be generated when packages are not set")
    }

    @Test
    fun `should fail when input file is missing`() {
        val mojo = AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputFile(File("src/test/resources/non_existent.yaml"))
            codegenOutputDirectory(outputPath("target/should-fail"))
            resourceOutputDirectory(outputPath("target/generated-resources/asyncapi"))
            models(models(packageName = "com.fail"))
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
            codegenOutputDirectory(outputPath("target/should-fail-gen"))
            resourceOutputDirectory(outputPath("target/generated-resources/asyncapi"))
            models(models(packageName = "com.fail"))
            generatorName("invalid-lang")
        }
        val exception =
            assertThrows<MojoExecutionException> {
                mojo.execute()
            }

        assertEquals(
            "Invalid generatorName 'invalid-lang'. Supported values: kotlin, java",
            exception.message,
        )
    }

    @Test
    fun `should fail when spring kafka mode is invalid`() {
        val mojo = AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputFile(inputPath("asyncapi_valid_content_kotlin.yaml"))
            codegenOutputDirectory(outputPath("target/should-fail-client-mode"))
            resourceOutputDirectory(outputPath("target/generated-resources/asyncapi"))
            models(models(packageName = "com.fail"))
            clients(clients(springKafka = springKafka(packageName = "com.fail.client", mode = "basic")))
            generatorName("kotlin")
        }
        assertThrows<MojoExecutionException> {
            mojo.execute()
        }
    }

    @Test
    fun `should fail when java model type is invalid`() {
        val mojo = AsyncApiGeneratorMojo().apply {
            project(MavenProject())
            inputFile(inputPath("asyncapi_valid_content_kotlin.yaml"))
            codegenOutputDirectory(outputPath("target/should-fail-java-model-type"))
            resourceOutputDirectory(outputPath("target/generated-resources/asyncapi"))
            models(models(packageName = "com.fail", javaModelType = "data"))
            generatorName("java")
        }
        val exception =
            assertThrows<MojoExecutionException> {
                mojo.execute()
            }

        assertEquals(
            "Invalid models.javaModelType 'data'. Supported values: class, record",
            exception.message,
        )
    }
}
