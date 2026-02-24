package dev.banking.asyncapi.generator.gradle.plugin

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AsyncApiPluginTest {

    @Test
    fun `should generate kotlin models from valid asyncapi yaml`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()

        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_valid_content_kotlin.yaml")
        val yamlFile = File(yamlUrl.toURI())
        val specsDir = File(projectDir, "specs").apply { mkdirs() }
        yamlFile.copyTo(File(specsDir, "api.yaml"), overwrite = true)

        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("specs/api.yaml"))
                  outputDir.set(layout.buildDirectory.dir("generated"))

                  modelPackage.set("com.example.model")
                  generatorName.set("kotlin")
              }"""
        )

        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)

        val outputDir = File(projectDir, "build/generated/src/main/kotlin/com/example/model")
        assertTrue(outputDir.exists(), "Output directory should exist")
        assertEquals(outputDir.list()?.isNotEmpty(), true, "Output directory should not be empty")

        GradleTestHelper.copyGeneratedOutputToProjectBuild(File(projectDir, "build/generated"))
    }

    @Test
    fun `should allow bundle-only output with no packages`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()
        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_kafka_complex.yaml")
        File(yamlUrl.toURI()).copyTo(File(projectDir, "api.yaml"))
        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("api.yaml"))
                  outputDir.set(layout.buildDirectory.dir("generated"))
                  outputFile.set(layout.buildDirectory.file("bundled.yaml"))
                  generatorName.set("kotlin")
                  // no modelPackage/clientPackage/schemaPackage set
              }"""
        )
        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        val bundledFile = File(projectDir, "build/bundled.yaml")
        assertTrue(bundledFile.exists(), "Bundled file should exist")
        assertTrue(bundledFile.length() > 0, "Bundled file should not be empty")
        val generatedPackageRoot = File(projectDir, "build/generated/src/main/kotlin/com")
        assertTrue(!generatedPackageRoot.exists(), "No code should be generated when packages are not set")
    }

    @Test
    fun `should fail if client type is set without client package`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()
        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_kafka_complex.yaml")
        File(yamlUrl.toURI()).copyTo(File(projectDir, "api.yaml"))
        GradleTestHelper.writeBuildScript(projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("api.yaml"))
                  outputDir.set(layout.buildDirectory.dir("generated"))
                  modelPackage.set("com.example.model")
                  generatorName.set("kotlin")
                  configOptions.set(mapOf(
                      "client.type" to "spring-kafka"
                  ))
              }""")
        val result = GradleTestHelper.runGradleAndFail(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.FAILED, result.task(":generateAsyncApi")?.outcome)
        assertTrue(result.output.contains("client.type requires clientPackage"))
    }

    @Test
    fun `should generate models only when no client or schema type is set`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()
        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_valid_content_kotlin.yaml")
        val yamlFile = File(yamlUrl.toURI())
        val specsDir = File(projectDir, "specs").apply { mkdirs() }
        yamlFile.copyTo(File(specsDir, "api.yaml"), overwrite = true)
        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("specs/api.yaml"))
                  outputDir.set(layout.buildDirectory.dir("generated"))
                  modelPackage.set("com.example.model")
                  clientPackage.set("com.example.client")
                  schemaPackage.set("com.example.schema")
                  generatorName.set("kotlin")
              }"""
        )
        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        val modelDir = File(projectDir, "build/generated/src/main/kotlin/com/example/model")
        val clientDir = File(projectDir, "build/generated/src/main/kotlin/com/example/client")
        val schemaDir = File(projectDir, "build/generated/src/main/kotlin/com/example/schema")
        assertTrue(modelDir.exists(), "Model directory should exist")
        assertTrue(!clientDir.exists(), "Client directory should not exist without client.type")
        assertTrue(!schemaDir.exists(), "Schema directory should not exist without schema.type")
    }

    @Test
    fun `should generate kotlin kafka client from generic kafka yaml`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()

        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_kafka_complex.yaml")
        val yamlFile = File(yamlUrl.toURI())
        val specsDir = File(projectDir, "specs").apply { mkdirs() }
        yamlFile.copyTo(File(specsDir, "api.yaml"), overwrite = true)

        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("specs/api.yaml"))
                  outputDir.set(layout.buildDirectory.dir("generated"))

                  modelPackage.set("com.example.kafka.model")
                  clientPackage.set("com.example.kafka.client")
                  generatorName.set("kotlin")
                  configOptions.set(mapOf(
                      "client.type" to "spring-kafka"
                  ))
              }"""
        )

        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)

        val clientDir = File(projectDir, "build/generated/src/main/kotlin/com/example/kafka/client")
        assertTrue(clientDir.exists(), "Client directory should exist")

        GradleTestHelper.copyGeneratedOutputToProjectBuild(File(projectDir, "build/generated"))
    }

    @Test
    fun `should generate java kafka client from generic kafka yaml`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()

        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_kafka_complex.yaml")
        val yamlFile = File(yamlUrl.toURI())
        val specsDir = File(projectDir, "specs").apply { mkdirs() }
        yamlFile.copyTo(File(specsDir, "api.yaml"), overwrite = true)

        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("specs/api.yaml"))
                  outputDir.set(layout.buildDirectory.dir("generated"))

                  modelPackage.set("com.example.kafka.model")
                  clientPackage.set("com.example.kafka.client")
                  generatorName.set("java")
                  configOptions.set(mapOf(
                      "client.type" to "spring-kafka"
                  ))
              }"""
        )

        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)

        val clientDir = File(projectDir, "build/generated/src/main/java/com/example/kafka/client")
        assertTrue(clientDir.exists(), "Client directory should exist")

        GradleTestHelper.copyGeneratedOutputToProjectBuild(File(projectDir, "build/generated"))
    }

    @Test
    fun `should write bundled output file if configured`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()
        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_kafka_complex.yaml")
        File(yamlUrl.toURI()).copyTo(File(projectDir, "api.yaml"))

        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("api.yaml"))
                  outputDir.set(layout.buildDirectory.dir("generated"))
                  outputFile.set(layout.buildDirectory.file("bundled.yaml")) // Configured output file

                  modelPackage.set("com.example.bundled")
                  generatorName.set("kotlin")
              }"""
        )

        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)

        val bundledFile = File(projectDir, "build/bundled.yaml")
        assertTrue(bundledFile.exists(), "Bundled file should exist")
        assertTrue(bundledFile.length() > 0, "Bundled file should not be empty")
    }

    @Test
    fun `should generate avro schema when schema type is avro`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()
        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_kafka_complex.yaml")
        val yamlFile = File(yamlUrl.toURI())
        val specsDir = File(projectDir, "specs").apply { mkdirs() }
        yamlFile.copyTo(File(specsDir, "api.yaml"), overwrite = true)
        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("specs/api.yaml"))
                  outputDir.set(layout.buildDirectory.dir("generated"))
                  modelPackage.set("com.example.avro.model")
                  schemaPackage.set("com.example.avro.schema")
                  generatorName.set("kotlin")
                  configOptions.set(mapOf(
                      "schema.type" to "avro"
                  ))
              }"""
        )
        val result = GradleTestHelper.runGradle(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateAsyncApi")?.outcome)
        val schemaDir = File(projectDir, "build/generated/src/main/kotlin/com/example/avro/schema")
        assertTrue(schemaDir.exists(), "Schema directory should exist")
    }

    @Test
    fun `should fail if input file is missing`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()

        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("missing.yaml"))
                  outputDir.set(layout.buildDirectory.dir("generated"))
                  modelPackage.set("com.example.fail")
                  generatorName.set("kotlin")
              }"""
        )

        val result = GradleTestHelper.runGradleAndFail(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.FAILED, result.task(":generateAsyncApi")?.outcome)
        assertTrue(result.output.contains("java.io.FileNotFoundException") || result.output.contains("missing.yaml"))
    }

    @Test
    fun `should fail if generator name is invalid`() {
        val projectDir = Files.createTempDirectory("gradleTest").toFile()
        val yamlUrl = GradleTestHelper.resourceFile("asyncapi_valid_content_kotlin.yaml")
        File(yamlUrl.toURI()).copyTo(File(projectDir, "api.yaml"))

        GradleTestHelper.writeBuildScript(
            projectDir, """
              plugins { id("dev.banking.asyncapi.generator") }
              asyncapiGenerate {
                  inputFile.set(file("api.yaml"))
                  outputDir.set(layout.buildDirectory.dir("generated"))
                  modelPackage.set("com.example.fail")
                  generatorName.set("python") // Invalid
              }"""
        )

        val result = GradleTestHelper.runGradleAndFail(projectDir, "generateAsyncApi")
        assertEquals(TaskOutcome.FAILED, result.task(":generateAsyncApi")?.outcome)
        assertTrue(result.output.contains("Invalid generatorName 'python'"))
    }
}
