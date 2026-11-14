package dev.banking.asyncapi.generator.systemtest

import java.io.File

/**
 * Builds a real Gradle consumer project from the scenario's `gradle/build.gradle.kts`
 * or, for the Groovy DSL, `gradle/build.gradle`.
 *
 * The build file refers to plugins and libraries through the `libs` version catalog,
 * written from [SystemTestEnvironment.buildFileVersions].
 *
 * The project gets its own wrapper pinned to [SystemTestEnvironment.gradleVersion],
 * so the Gradle version matrix needs no other change. Only the staged repository
 * may serve generator artifacts; everything else resolves from public repositories.
 *
 * @param environment Build-provided settings.
 * @param runner Process runner shared by all frontends.
 * @param dsl Build script language.
 * @param tasks Tasks [run] executes; `build` generates, compiles, and runs the checks.
 */
internal class GradleFrontend(
    private val environment: SystemTestEnvironment,
    private val runner: ProcessRunner,
    private val dsl: GradleDsl = GradleDsl.KOTLIN,
    private val tasks: List<String> = listOf("build"),
) {
    /** Prepares the project for [scenario] and runs [tasks] without checking the exit code. */
    fun run(scenario: Scenario, projectDirectory: File): FrontendRun {
        prepare(scenario, projectDirectory)
        val result = gradle(projectDirectory, tasks, "build.log")
        return FrontendRun.of(scenario, projectDirectory, result) { "build/generated/asyncapi/${it.id}" }
    }

    /** Writes a fresh consumer project for [scenario] into [projectDirectory]. */
    fun prepare(scenario: Scenario, projectDirectory: File) {
        ConsumerProject.prepare(scenario, environment, projectDirectory)
        installWrapper(projectDirectory)
        projectDirectory.resolve(dsl.settingsFile).writeText(settings(scenario))
        ConsumerProject.copyBuildFile(scenario, environment, "gradle/${dsl.buildFile}", projectDirectory)
        projectDirectory.resolve("gradle/libs.versions.toml").writeText(versionCatalog())
        projectDirectory.resolve("gradle.properties").writeText(
            "org.gradle.daemon.idletimeout=600000\norg.gradle.console=plain\n",
        )
    }

    /** Runs Gradle with [arguments] in a prepared project, logging to [logName], without checking the exit code. */
    fun gradle(projectDirectory: File, arguments: List<String>, logName: String): ProcessResult =
        runner.run(
            listOf(projectDirectory.resolve("gradlew").path) + arguments + "--stacktrace",
            projectDirectory,
            projectDirectory.resolve(logName),
            environment.buildToolEnvironment,
        )

    private fun installWrapper(projectDirectory: File) {
        val source = environment.gradleWrapperSource
        source.resolve("gradlew").copyTo(projectDirectory.resolve("gradlew")).setExecutable(true)
        source.resolve("gradle/wrapper/gradle-wrapper.jar")
            .copyTo(projectDirectory.resolve("gradle/wrapper/gradle-wrapper.jar"))
        projectDirectory.resolve("gradle/wrapper/gradle-wrapper.properties").writeText(
            """
            |distributionBase=GRADLE_USER_HOME
            |distributionPath=wrapper/dists
            |distributionUrl=https\://services.gradle.org/distributions/gradle-${environment.gradleVersion}-bin.zip
            |zipStoreBase=GRADLE_USER_HOME
            |zipStorePath=wrapper/dists
            |""".trimMargin(),
        )
    }

    private fun settings(scenario: Scenario): String {
        val stagedRepository = """
            |        maven {
            |            url = uri(${dsl.literal(environment.repository.path)})
            |            content { includeGroup(${dsl.literal(GENERATOR_GROUP)}) }
            |        }
            """.trimMargin()
        return """
            |pluginManagement {
            |    repositories {
            |$stagedRepository
            |        gradlePluginPortal()
            |    }
            |}
            |
            |dependencyResolutionManagement {
            |    repositories {
            |$stagedRepository
            |        mavenCentral()
            |    }
            |}
            |
            |rootProject.name = ${dsl.literal(scenario.name)}
            |""".trimMargin()
    }

    private fun versionCatalog(): String {
        val versions = environment.buildFileVersions.entries.joinToString("\n") { (name, version) -> "$name = \"$version\"" }
        return """
            |[versions]
            |$versions
            |
            |[libraries]
            |kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
            |kotlin-test-junit5 = { module = "org.jetbrains.kotlin:kotlin-test-junit5", version.ref = "kotlin" }
            |jakarta-validation-api = { module = "jakarta.validation:jakarta.validation-api", version.ref = "jakarta-validation" }
            |jackson-annotations = { module = "com.fasterxml.jackson.core:jackson-annotations", version.ref = "jackson-annotations" }
            |jackson-databind = { module = "com.fasterxml.jackson.core:jackson-databind", version.ref = "jackson" }
            |jackson-dataformat-yaml = { module = "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml", version.ref = "jackson" }
            |jackson-module-kotlin = { module = "com.fasterxml.jackson.module:jackson-module-kotlin", version.ref = "jackson" }
            |spring-kafka = { module = "org.springframework.kafka:spring-kafka", version.ref = "spring-kafka" }
            |avro = { module = "org.apache.avro:avro", version.ref = "avro" }
            |protobuf-java = { module = "com.google.protobuf:protobuf-java", version.ref = "protobuf" }
            |protobuf-kotlin = { module = "com.google.protobuf:protobuf-kotlin", version.ref = "protobuf" }
            |junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            |junit-platform-launcher = { module = "org.junit.platform:junit-platform-launcher", version.ref = "junit" }
            |
            |[plugins]
            |kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
            |asyncapi-generator = { id = "$GENERATOR_GROUP", version.ref = "asyncapi-generator" }
            |""".trimMargin()
    }

    private companion object {
        const val GENERATOR_GROUP = "dev.banking.asyncapi.generator"
    }
}

/** Build script language of a Gradle consumer project. */
internal enum class GradleDsl(val buildFile: String, val settingsFile: String) {
    KOTLIN("build.gradle.kts", "settings.gradle.kts"),
    GROOVY("build.gradle", "settings.gradle"),
    ;

    /** A string literal that the script uses verbatim, without interpolation. */
    fun literal(value: String): String = when (this) {
        KOTLIN -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$") + "\""
        GROOVY -> "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'"
    }
}
