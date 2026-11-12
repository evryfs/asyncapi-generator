package dev.banking.asyncapi.generator.systemtest

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * Runs every scenario through the CLI, the Maven plugin, and the Gradle plugin.
 *
 * Maven and Gradle builds compile the generated code against the project's
 * shared library versions and run the scenario's checks; a failing check fails the build.
 * All three frontends must then have produced identical output.
 */
class ScenarioSystemTest {
    private val environment = SystemTestEnvironment.current
    private val runner = ProcessRunner(environment.processTimeout)
    private val cli = CliFrontend(environment, runner)
    private val maven = MavenFrontend(environment, runner)
    private val gradle = GradleFrontend(environment, runner)

    @TestFactory
    fun scenarios(): List<DynamicContainer> = scenarios.map { scenario ->
        val work = environment.workDirectory.resolve(scenario.name)
        val outputs = linkedMapOf<String, GeneratedOutput>()

        dynamicContainer(
            scenario.name,
            listOf(
                dynamicTest("Maven build generates, compiles, and passes checks") {
                    outputs["maven"] = GeneratedOutput.read(maven.build(scenario, work.resolve("maven")))
                },
                dynamicTest("Gradle build generates, compiles, and passes checks") {
                    outputs["gradle"] = GeneratedOutput.read(gradle.build(scenario, work.resolve("gradle")))
                },
                dynamicTest("CLI generates") {
                    outputs["cli"] = GeneratedOutput.read(cli.generate(scenario, work.resolve("cli")))
                },
                dynamicTest("all frontends generate identical output") {
                    check(outputs.size == 3) { "Skipped: a frontend failed to generate (${outputs.keys})" }
                    assertSameOutput(outputs)
                },
            ),
        )
    }
}
