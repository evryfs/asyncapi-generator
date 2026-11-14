package dev.banking.asyncapi.generator.systemtest

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regenerating after a schema is removed from the contract, as a developer does without a clean build.
 *
 * Files the generator wrote for the removed schema must disappear, so they no longer compile
 * into the consumer; files the generator did not write must stay.
 */
class StaleGeneratedFilesSystemTest {
    private val environment = SystemTestEnvironment.current
    private val runner = ProcessRunner(environment.processTimeout)
    private val maven = MavenFrontend(environment, runner)
    private val gradle = GradleFrontend(environment, runner)

    @TestFactory
    fun `regeneration removes files of a removed schema`(): List<DynamicTest> = listOf(
        dynamicTest("Maven") {
            val project = environment.workDirectory.resolve("${SCENARIO.name}/maven")
            maven.prepare(SCENARIO, project)
            assertRemovesStaleFiles(project, "target/generated-sources/asyncapi") { log ->
                maven.mvn(project, listOf("generate-sources"), log)
            }
        },
        dynamicTest("Gradle") {
            val project = environment.workDirectory.resolve("${SCENARIO.name}/gradle")
            gradle.prepare(SCENARIO, project)
            assertRemovesStaleFiles(project, "build/generated/asyncapi/models") { log ->
                gradle.gradle(project, listOf("generateAsyncApi"), log)
            }
        },
    )

    private fun assertRemovesStaleFiles(project: File, outputDirectory: String, generate: (String) -> ProcessResult) {
        val packageDirectory = project.resolve("$outputDirectory/com/example/stale")
        generate("first.log").assertSuccess("The first generation")
        assertTrue(packageDirectory.resolve("LegacyAccountCreated.kt").exists(), "the first generation writes the legacy model")
        val handWritten = packageDirectory.resolve("HandWritten.kt").apply { writeText("package com.example.stale\n") }

        val spec = project.resolve("${ConsumerProject.SPEC_DIRECTORY}/asyncapi.yaml")
        spec.writeText(spec.readText().replace(STALE_SECTION, ""))
        generate("second.log").assertSuccess("The generation after removing a schema")

        assertFalse(packageDirectory.resolve("LegacyAccountCreated.kt").exists(), "the removed schema's model is deleted")
        assertTrue(packageDirectory.resolve("AccountOpened.kt").exists(), "models still in the contract are kept")
        assertTrue(handWritten.exists(), "files the generator did not write are kept")
    }

    private companion object {
        val STALE_SECTION = Regex("""\n\s*# stale:start.*?# stale:end""", RegexOption.DOT_MATCHES_ALL)

        val SCENARIO = Scenario(
            name = "stale-generated-files",
            executions = listOf(Execution(id = "models", generatorName = "kotlin", modelPackage = "com.example.stale")),
        )
    }
}
