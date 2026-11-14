package dev.banking.asyncapi.generator.systemtest

import java.io.File

/**
 * Builds a real Maven consumer project from the scenario's `maven/pom.xml`.
 *
 * The pom refers to versions as `${<name>.version}` properties, which are passed on
 * the command line from [SystemTestEnvironment.buildFileVersions].
 *
 * The staged repository is the build's local repository, so only the artifacts
 * under test can be resolved for the generator. The developer's own local
 * repository is declared as a read-only remote so third-party downloads stay fast.
 *
 * @param environment Build-provided settings.
 * @param runner Process runner shared by all frontends.
 */
internal class MavenFrontend(
    private val environment: SystemTestEnvironment,
    private val runner: ProcessRunner,
) {
    /** Generates, compiles, and runs the checks for [scenario] without checking the exit code. */
    fun run(scenario: Scenario, projectDirectory: File): FrontendRun {
        prepare(scenario, projectDirectory)
        val result = mvn(projectDirectory, listOf("verify"), "build.log")
        return FrontendRun.of(scenario, projectDirectory, result) { DEFAULT_OUTPUT_DIRECTORY }
    }

    /** Writes a fresh consumer project for [scenario] into [projectDirectory]. */
    fun prepare(scenario: Scenario, projectDirectory: File) {
        ConsumerProject.prepare(scenario, environment, projectDirectory)
        ConsumerProject.copyBuildFile(scenario, environment, "maven/pom.xml", projectDirectory)
        projectDirectory.resolve("settings.xml").writeText(settings())
    }

    /** Runs Maven with [goals] in a prepared project, logging to [logName], without checking the exit code. */
    fun mvn(projectDirectory: File, goals: List<String>, logName: String): ProcessResult =
        runner.run(
            listOf(
                environment.mavenHome.resolve("bin/mvn").path,
                "--batch-mode",
                "--no-transfer-progress",
                "--settings", "settings.xml",
                "-Dmaven.repo.local=${environment.repository.path}",
            ) + environment.buildFileVersions.map { (name, version) -> "-D$name.version=$version" } + goals,
            projectDirectory,
            projectDirectory.resolve(logName),
            environment.buildToolEnvironment,
        )

    private fun settings(): String = """
        |<?xml version="1.0" encoding="UTF-8"?>
        |<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0">
        |    <profiles>
        |        <profile>
        |            <id>user-repository-cache</id>
        |            <repositories>
        |                <repository>
        |                    <id>user-repository-cache</id>
        |                    <url>${environment.userRepository.toURI()}</url>
        |                    <snapshots><enabled>false</enabled></snapshots>
        |                </repository>
        |            </repositories>
        |            <pluginRepositories>
        |                <pluginRepository>
        |                    <id>user-repository-cache</id>
        |                    <url>${environment.userRepository.toURI()}</url>
        |                    <snapshots><enabled>false</enabled></snapshots>
        |                </pluginRepository>
        |            </pluginRepositories>
        |        </profile>
        |    </profiles>
        |    <activeProfiles>
        |        <activeProfile>user-repository-cache</activeProfile>
        |    </activeProfiles>
        |</settings>
        |""".trimMargin()

    private companion object {
        /** The Maven plugin's default `outputDirectory`. */
        const val DEFAULT_OUTPUT_DIRECTORY = "target/generated-sources/asyncapi"
    }
}
