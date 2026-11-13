package dev.banking.asyncapi.generator.systemtest

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Repeated Gradle builds of one consumer project, as a developer runs them.
 *
 * Generation must work with the configuration cache, be skipped while nothing
 * changed, and rerun when any file of the contract changes, including files
 * that are only reachable through an external `$ref`.
 */
class GradleIncrementalBuildSystemTest {
    private val environment = SystemTestEnvironment.current
    private val gradle = GradleFrontend(environment, ProcessRunner(environment.processTimeout))

    @Test
    fun `generation is cached until a referenced contract file changes`() {
        val project = environment.workDirectory.resolve(SCENARIO.name)
        gradle.prepare(SCENARIO, project)
        val generatedUser = project.resolve("build/generated/asyncapi/models/com/example/incremental/User.kt")
        val referencedFile = project.resolve("${ConsumerProject.SPEC_DIRECTORY}/schema-fragment/properties.yaml")

        build(project, "first").also { output ->
            assertTrue(output.ran(TASK), "the first build generates")
        }
        val firstGeneration = generatedUser.readText()

        build(project, "unchanged").also { output ->
            assertTrue(CACHE_REUSED in output, "an unchanged build reuses the configuration cache")
            assertFalse(output.ran(TASK), "an unchanged build skips generation")
        }

        referencedFile.writeText(referencedFile.readText().replace("type: integer", "type: string"))
        build(project, "changed").also { output ->
            assertTrue(CACHE_REUSED in output, "a contract change does not invalidate the configuration cache")
            assertTrue(output.ran(TASK), "changing a referenced file regenerates")
        }
        assertNotEquals(firstGeneration, generatedUser.readText(), "regenerated code reflects the change")
    }

    private fun build(project: File, name: String): String {
        val result = gradle.gradle(project, listOf("generateAsyncApi", "--configuration-cache"), "$name.log")
        result.assertSuccess("The $name build")
        return result.output
    }

    /** Whether Gradle's plain console reports [task] as executed rather than skipped. */
    private fun String.ran(task: String): Boolean =
        lines().any { it.trim() == "> Task $task" }

    private companion object {
        const val TASK = ":generateModelsAsyncApi"
        const val CACHE_REUSED = "Reusing configuration cache."

        val SCENARIO = Scenario(
            name = "gradle-incremental-build",
            files = "bundling",
            executions = listOf(
                Execution(
                    id = "models",
                    generatorName = "kotlin",
                    inputSpec = "schema-fragment/main.yaml",
                    modelPackage = "com.example.incremental",
                ),
            ),
        )
    }
}
