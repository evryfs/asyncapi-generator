package dev.banking.asyncapi.generator.systemtest

import java.io.File

/**
 * Builds a real Gradle consumer project that applies the staged Gradle plugin.
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
        projectDirectory.resolve(dsl.fileName("settings.gradle")).writeText(settings(scenario))
        projectDirectory.resolve(dsl.fileName("build.gradle")).writeText(buildScript(scenario))
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
            |rootProject.name = ${dsl.literal(ConsumerProject.projectName(scenario))}
            |""".trimMargin()
    }

    private fun buildScript(scenario: Scenario): String = """
        |plugins {
        |    id(${dsl.literal("org.jetbrains.kotlin.jvm")}) version ${dsl.literal(environment.libraries.kotlin)}
        |    id(${dsl.literal(GENERATOR_GROUP)}) version ${dsl.literal(environment.version)}
        |}
        |
        |dependencies {
        |${dependencyDeclarations(ConsumerDependencies(environment.libraries, scenario.libraries))}
        |}
        |
        |java {
        |    sourceCompatibility = JavaVersion.VERSION_21
        |    targetCompatibility = JavaVersion.VERSION_21
        |}
        |
        |kotlin {
        |    compilerOptions {
        |        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        |    }
        |}
        |
        |${dsl.testTasks} {
        |    useJUnitPlatform()
        |}
        |
        |asyncApiGenerator {
        |${scenario.client?.let(::clientConfiguration).orEmpty()}
        |    executions {
        |${scenario.executions.joinToString("\n", transform = ::execution)}
        |    }
        |}
        |""".trimMargin()

    private fun dependencyDeclarations(dependencies: ConsumerDependencies): String =
        listOf(
            "implementation" to dependencies.implementation,
            "testImplementation" to dependencies.test,
            "testRuntimeOnly" to dependencies.gradleTestRuntime,
        ).flatMap { (configuration, declared) -> declared.map { "    $configuration(${dsl.literal(it.toString())})" } }
            .joinToString("\n")

    private fun execution(execution: Execution): String = buildList {
        add("register(${dsl.literal(execution.id)}) {")
        add("    generatorName.set(${dsl.literal(execution.generatorName)})")
        add("    inputSpec.set(file(${dsl.literal(ConsumerProject.inputSpecPath(execution))}))")
        execution.outputDirectory?.let {
            add("    outputDirectory.set(layout.projectDirectory.dir(${dsl.literal(it)}))")
        }
        execution.outputFile?.let { add("    outputFile.set(layout.projectDirectory.file(${dsl.literal(it)}))") }
        execution.modelPackage?.let { add("    modelPackage.set(${dsl.literal(it)})") }
        execution.clientPackage?.let { add("    clientPackage.set(${dsl.literal(it)})") }
        execution.schemaPackage?.let { add("    schemaPackage.set(${dsl.literal(it)})") }
        if (execution.modelType != null || execution.modelAnnotation != null) {
            add("    modelConfig {")
            execution.modelType?.let { add("        modelType.set(${dsl.literal(it)})") }
            execution.modelAnnotation?.let { add("        modelAnnotation.set(${dsl.literal(it)})") }
            add("    }")
        }
        execution.suppressedWarnings.forEach { add("    suppressedWarnings.add(${dsl.literal(it)})") }
        add("}")
    }.joinToString("\n") { "        $it" }

    private fun clientConfiguration(client: ClientConfig): String = buildList {
        add("clientConfig {")
        add("    clientType.set(${dsl.literal(client.clientType)})")
        add("    clientContract.set(${dsl.literal(client.clientContract)})")
        add("    producer {")
        add("        enabled.set(${client.producerEnabled})")
        if (client.producerAdditionalPayloadTypes.isNotEmpty()) {
            add("        additionalPayloadTypes.set(${dsl.list(client.producerAdditionalPayloadTypes)})")
        }
        add("    }")
        add("    consumer {")
        add("        enabled.set(${client.consumerEnabled})")
        add("    }")
        client.topicParameterProperties.forEach { (parameter, property) ->
            add("    topicParameterProperties.put(${dsl.literal(parameter)}, ${dsl.literal(property)})")
        }
        if (client.clientContractValidationAnnotation != null || client.payloadParameterValidationAnnotation != null) {
            add("    validationAnnotations {")
            client.clientContractValidationAnnotation?.let { add("        clientContract.set(${dsl.literal(it)})") }
            client.payloadParameterValidationAnnotation?.let { add("        payloadParameter.set(${dsl.literal(it)})") }
            add("    }")
        }
        add("}")
    }.joinToString("\n") { "    $it" }

    private companion object {
        const val GENERATOR_GROUP = "dev.banking.asyncapi.generator"
    }
}

/** Build script language of a Gradle consumer project. */
internal enum class GradleDsl(private val extension: String, val testTasks: String) {
    KOTLIN(".kts", "tasks.withType<Test>()"),
    GROOVY("", "tasks.withType(Test)"),
    ;

    fun fileName(baseName: String): String = baseName + extension

    /** A string literal that the script uses verbatim, without interpolation. */
    fun literal(value: String): String = when (this) {
        KOTLIN -> value.kotlinStringLiteral()
        GROOVY -> "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'"
    }

    fun list(values: List<String>): String {
        val literals = values.joinToString { literal(it) }
        return when (this) {
            KOTLIN -> "listOf($literals)"
            GROOVY -> "[$literals]"
        }
    }
}
