package dev.banking.asyncapi.generator.systemtest

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Runs every scenario through the Maven plugin, the Gradle plugin (Kotlin and Groovy DSL), and the CLI.
 *
 * Maven and Gradle Kotlin DSL builds compile the generated code against the project's
 * shared library versions and run the scenario's checks; a failing check fails the build.
 * The Groovy DSL build and the CLI only generate. All frontends must then have produced
 * identical output, and code generated from a bundled document must match code generated
 * from the original contract. Scenarios with an [ExpectedFailure] instead require every
 * frontend to report it.
 */
class ScenarioSystemTest {
    private val environment = SystemTestEnvironment.current
    private val runner = ProcessRunner(environment.processTimeout)
    private val cli = CliFrontend(environment, runner)
    private val frontends = listOf(
        Frontend("Maven", "maven", buildsConsumer = true, MavenFrontend(environment, runner)::run),
        Frontend("Gradle", "gradle", buildsConsumer = true, GradleFrontend(environment, runner)::run),
        Frontend(
            "Gradle Groovy DSL",
            "gradle-groovy",
            buildsConsumer = false,
            GradleFrontend(environment, runner, GradleDsl.GROOVY, listOf("generateAsyncApi"))::run,
        ),
        Frontend("CLI", "cli", buildsConsumer = false, cli::run),
    )

    /**
     * @property directory Work subdirectory of a scenario for this frontend.
     * @property buildsConsumer Whether [run] also compiles the output and runs the scenario checks.
     */
    private class Frontend(
        val name: String,
        val directory: String,
        val buildsConsumer: Boolean,
        val run: (Scenario, File) -> FrontendRun,
    )

    @TestFactory
    fun scenarios(): List<DynamicContainer> = scenarios.map { scenario ->
        val work = environment.workDirectory.resolve(scenario.name)
        val tests = scenario.expectedFailure
            ?.let { failure -> failureTests(scenario, failure, work) }
            ?: successTests(scenario, work)
        val unmatched = scenario.knownIssues.keys - tests.map(DynamicTest::getDisplayName).toSet()
        check(unmatched.isEmpty()) { "Scenario ${scenario.name} lists known issues for tests it does not have: $unmatched" }
        dynamicContainer(scenario.name, tests.map { it.withKnownIssue(scenario) })
    }

    /** Inverts a test listed in [Scenario.knownIssues]: it passes while the bug is still there. */
    private fun DynamicTest.withKnownIssue(scenario: Scenario): DynamicTest {
        val issue = scenario.knownIssues[displayName] ?: return this
        return dynamicTest("$displayName (known issue: $issue)") {
            val failure = runCatching { executable.execute() }.exceptionOrNull()
            if (failure == null) {
                throw AssertionError("Known issue '$issue' no longer reproduces; remove it from scenario ${scenario.name}")
            }
        }
    }

    private fun successTests(scenario: Scenario, work: File): List<DynamicTest> {
        val runs = linkedMapOf<String, FrontendRun>()
        val frontendTests = frontends.map { frontend ->
            val verb = if (frontend.buildsConsumer) "generates, compiles, and passes checks" else "generates"
            dynamicTest("${frontend.name} $verb") {
                val frontendRun = frontend.run(scenario, work.resolve(frontend.directory))
                frontendRun.result.assertSuccess(frontend.name)
                assertNoArtifactsWithoutPackages(scenario, frontendRun)
                runs[frontend.name] = frontendRun
            }
        }
        fun allRuns(): Map<String, FrontendRun> {
            check(runs.size == frontends.size) { "Skipped: a frontend failed to generate (succeeded: ${runs.keys})" }
            return runs
        }
        val identicalOutput = dynamicTest("all frontends generate identical output") {
            assertSameOutput(allRuns().mapValues { (_, run) -> GeneratedOutput.read(run) })
        }
        val warnings = scenario.expectedWarnings.map { warning ->
            dynamicTest("every frontend warns '$warning'") {
                allRuns().forEach { (name, run) ->
                    assertTrue(warning in run.result.output, "$name output lacks '$warning'. Full log: ${run.result.log}")
                }
            }
        }
        val absentWarnings = scenario.absentWarnings.map { warning ->
            dynamicTest("no frontend warns '$warning'") {
                allRuns().forEach { (name, run) ->
                    assertFalse(warning in run.result.output, "$name output contains '$warning'. Full log: ${run.result.log}")
                }
            }
        }
        val roundTrips = scenario.executions
            .filter { it.outputFile != null && it.generatesArtifacts }
            .map { execution ->
                dynamicTest("code generated from the bundled document of '${execution.id}' matches the original") {
                    assertBundleRoundTrip(scenario, execution, work.resolve("round-trip/${execution.id}"))
                }
            }
        return frontendTests + identicalOutput + warnings + absentWarnings + roundTrips
    }

    private fun failureTests(scenario: Scenario, failure: ExpectedFailure, work: File): List<DynamicTest> =
        frontends.map { frontend ->
            val name = frontend.name
            dynamicTest("$name fails with the expected diagnostic") {
                val result = frontend.run(scenario, work.resolve(frontend.directory)).result
                assertNotEquals(0, result.exitCode, "$name was expected to fail. Full log: ${result.log}")
                failure.messages.forEach { message ->
                    assertTrue(
                        message in result.output,
                        "$name output lacks '$message'. Full log: ${result.log}\n${result.tail()}",
                    )
                }
                failure.absent.forEach { text ->
                    assertFalse(text in result.output, "$name output contains '$text'. Full log: ${result.log}")
                }
            }
        }

    /**
     * Generates [execution] from the original contract, then again from the bundled
     * document it wrote, and requires both to produce the same code.
     */
    private fun assertBundleRoundTrip(scenario: Scenario, execution: Execution, project: File) {
        ConsumerProject.prepare(scenario, environment, project)
        val bundleName = "round-trip-bundle.${File(checkNotNull(execution.outputFile)).extension}"
        val original = execution.copy(
            id = "${execution.id}-original",
            outputDirectory = "original",
            outputFile = bundleName,
        )
        cli.generate(original, scenario.client, project, scenario.sharedClientConfig).assertSuccess("Generating from the original contract")
        project.resolve(bundleName).copyTo(project.resolve(ConsumerProject.SPEC_DIRECTORY).resolve(bundleName))

        val fromBundle = execution.copy(
            id = "${execution.id}-from-bundle",
            inputSpec = bundleName,
            outputDirectory = "from-bundle",
            outputFile = null,
        )
        cli.generate(fromBundle, scenario.client, project, scenario.sharedClientConfig).assertSuccess("Generating from the bundled document")

        assertSameOutput(
            linkedMapOf(
                "the original contract" to GeneratedOutput(GeneratedOutput.readDirectory(project.resolve("original"))),
                "the bundled document" to GeneratedOutput(GeneratedOutput.readDirectory(project.resolve("from-bundle"))),
            ),
        )
    }
}
