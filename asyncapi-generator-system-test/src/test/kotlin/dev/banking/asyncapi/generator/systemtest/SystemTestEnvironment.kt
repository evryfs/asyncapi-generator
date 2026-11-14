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
 * @property libraries Versions of the libraries consumer builds compile generated code against,
 *   taken from the project's root pom so the whole project shares one set.
 * @property gradleVersion Gradle version that consumer builds run on.
 * @property gradleWrapperSource Directory holding the `gradlew` scripts and wrapper jar to copy.
 * @property scenarios Directory holding scenario specs and checks.
 * @property checkSupport Kotlin helpers copied next to every scenario's checks.
 * @property workDirectory Scratch directory for consumer projects, output, and logs.
 * @property processTimeout Upper bound for a single CLI, Maven, or Gradle process.
 */
internal data class SystemTestEnvironment(
    val version: String,
    val repository: File,
    val userRepository: File,
    val mavenHome: File,
    val libraries: LibraryVersions,
    val gradleVersion: String,
    val gradleWrapperSource: File,
    val scenarios: File,
    val checkSupport: File,
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
                libraries = LibraryVersions(
                    kotlin = property("kotlin.version"),
                    springKafka = property("spring-kafka.version"),
                    jakartaValidation = property("jakarta-validation.version"),
                    jacksonAnnotations = property("jackson-annotations.version"),
                    junit = property("junit.version"),
                    jackson = property("jackson.version"),
                    avro = property("avro.version"),
                    protobuf = property("protobuf.version"),
                ),
                gradleVersion = property("gradle.version"),
                gradleWrapperSource = File(property("gradle-wrapper")),
                scenarios = File(property("scenarios")),
                checkSupport = File(property("check-support")),
                workDirectory = File(property("work")),
                processTimeout = Duration.ofSeconds(property("process-timeout-seconds").toLong()),
            )
        }

        private fun property(name: String): String =
            System.getProperty("system-test.$name")
                ?: error("Missing system property 'system-test.$name'. Run with: mvn verify -Psystem-test")
    }
}

/**
 * Library versions shared by the whole project, as declared in the root pom.
 *
 * @property kotlin Kotlin standard library and test library version.
 * @property springKafka Spring Kafka version; brings Spring Messaging, Spring Context, and Kafka clients.
 * @property jakartaValidation Jakarta Validation API version.
 * @property jacksonAnnotations Jackson annotations version (minor-only since Jackson 2.20).
 * @property junit JUnit version for the scenario checks.
 * @property jackson Jackson version the checks use to read documents and schemas.
 * @property avro Apache Avro version for generated SpecificRecords.
 * @property protobuf Protobuf Java and Kotlin runtime version for generated messages.
 */
internal data class LibraryVersions(
    val kotlin: String,
    val springKafka: String,
    val jakartaValidation: String,
    val jacksonAnnotations: String,
    val junit: String,
    val jackson: String,
    val avro: String,
    val protobuf: String,
)
