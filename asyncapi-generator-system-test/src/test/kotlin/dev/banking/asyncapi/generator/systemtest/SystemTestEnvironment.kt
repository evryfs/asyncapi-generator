package dev.banking.asyncapi.generator.systemtest

import java.io.File
import java.time.Duration

/**
 * Build-provided settings that every system test reads.
 *
 * Values come from Failsafe system properties declared in the module pom, so a
 * version matrix only needs `-Dsystem-test.<name>=<value>` on the command line.
 *
 * @property version Generator version staged into [repository].
 * @property repository Isolated local repository holding the staged frontends.
 * @property userRepository The developer's local repository, used read-only as a download cache.
 * @property mavenHome Maven installation that runs Maven consumer builds.
 * @property kotlinVersion Kotlin version used by consumer builds.
 * @property springBootVersion Spring Boot BOM version that consumer builds compile against.
 * @property gradleVersion Gradle version that consumer builds run on.
 * @property gradleWrapperSource Directory holding the `gradlew` scripts and wrapper jar to copy.
 * @property scenarios Directory holding scenario specs and checks.
 * @property workDirectory Scratch directory for consumer projects, output, and logs.
 * @property processTimeout Upper bound for a single CLI, Maven, or Gradle process.
 */
internal data class SystemTestEnvironment(
    val version: String,
    val repository: File,
    val userRepository: File,
    val mavenHome: File,
    val kotlinVersion: String,
    val springBootVersion: String,
    val gradleVersion: String,
    val gradleWrapperSource: File,
    val scenarios: File,
    val workDirectory: File,
    val processTimeout: Duration,
) {
    val cliJar: File
        get() = repository.resolve(
            "dev/banking/asyncapi/generator/asyncapi-generator-cli/$version/asyncapi-generator-cli-$version.jar",
        )

    /** Runs consumer builds on the same JDK as the system tests, regardless of the caller's shell. */
    val buildToolEnvironment: Map<String, String>
        get() = mapOf("JAVA_HOME" to System.getProperty("java.home"))

    companion object {
        val current: SystemTestEnvironment by lazy {
            SystemTestEnvironment(
                version = property("version"),
                repository = File(property("repository")),
                userRepository = File(property("user-repository")),
                mavenHome = File(property("maven-home")),
                kotlinVersion = property("kotlin.version"),
                springBootVersion = property("spring-boot.version"),
                gradleVersion = property("gradle.version"),
                gradleWrapperSource = File(property("gradle-wrapper")),
                scenarios = File(property("scenarios")),
                workDirectory = File(property("work")),
                processTimeout = Duration.ofSeconds(property("process-timeout-seconds").toLong()),
            )
        }

        private fun property(name: String): String =
            System.getProperty("system-test.$name")
                ?: error("Missing system property 'system-test.$name'. Run with: mvn verify -Psystem-test")
    }
}
