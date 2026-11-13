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
 */
internal class GradleFrontend(
    private val environment: SystemTestEnvironment,
    private val runner: ProcessRunner,
) {
    /** Generates, compiles, and runs the checks for [scenario] without checking the exit code. */
    fun run(scenario: Scenario, projectDirectory: File): FrontendRun {
        ConsumerProject.prepare(scenario, environment, projectDirectory)
        installWrapper(projectDirectory)
        projectDirectory.resolve("settings.gradle.kts").writeText(settings(scenario))
        projectDirectory.resolve("build.gradle.kts").writeText(buildScript(scenario))
        projectDirectory.resolve("gradle.properties").writeText(
            "org.gradle.daemon.idletimeout=600000\norg.gradle.console=plain\n",
        )

        val command = listOf(projectDirectory.resolve("gradlew").path, "build", "--stacktrace")
        val result = runner.run(
            command,
            projectDirectory,
            projectDirectory.resolve("build.log"),
            environment.buildToolEnvironment,
        )
        return FrontendRun.of(scenario, projectDirectory, result) { "build/generated/asyncapi/${it.id}" }
    }

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
            |            url = uri(${environment.repository.path.kotlinStringLiteral()})
            |            content { includeGroup("$GENERATOR_GROUP") }
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
            |rootProject.name = "${ConsumerProject.projectName(scenario)}"
            |""".trimMargin()
    }

    private fun buildScript(scenario: Scenario): String = """
        |plugins {
        |    kotlin("jvm") version "${environment.libraries.kotlin}"
        |    id("$GENERATOR_GROUP") version "${environment.version}"
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
        |tasks.test {
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
        ).flatMap { (configuration, declared) -> declared.map { "    $configuration(\"$it\")" } }
            .joinToString("\n")

    private fun execution(execution: Execution): String = buildList {
        add("register(${execution.id.kotlinStringLiteral()}) {")
        add("    generatorName.set(${execution.generatorName.kotlinStringLiteral()})")
        add("    inputSpec.set(file(${ConsumerProject.inputSpecPath(execution).kotlinStringLiteral()}))")
        execution.outputDirectory?.let {
            add("    outputDirectory.set(layout.projectDirectory.dir(${it.kotlinStringLiteral()}))")
        }
        execution.outputFile?.let { add("    outputFile.set(layout.projectDirectory.file(${it.kotlinStringLiteral()}))") }
        execution.modelPackage?.let { add("    modelPackage.set(${it.kotlinStringLiteral()})") }
        execution.clientPackage?.let { add("    clientPackage.set(${it.kotlinStringLiteral()})") }
        execution.schemaPackage?.let { add("    schemaPackage.set(${it.kotlinStringLiteral()})") }
        if (execution.modelType != null || execution.modelAnnotation != null) {
            add("    modelConfig {")
            execution.modelType?.let { add("        modelType.set(${it.kotlinStringLiteral()})") }
            execution.modelAnnotation?.let { add("        modelAnnotation.set(${it.kotlinStringLiteral()})") }
            add("    }")
        }
        add("}")
    }.joinToString("\n") { "        $it" }

    private fun clientConfiguration(client: ClientConfig): String = buildList {
        add("clientConfig {")
        add("    clientType.set(${client.clientType.kotlinStringLiteral()})")
        add("    clientContract.set(${client.clientContract.kotlinStringLiteral()})")
        add("    producer {")
        add("        enabled.set(${client.producerEnabled})")
        if (client.producerAdditionalPayloadTypes.isNotEmpty()) {
            val types = client.producerAdditionalPayloadTypes.joinToString { it.kotlinStringLiteral() }
            add("        additionalPayloadTypes.set(listOf($types))")
        }
        add("    }")
        add("    consumer {")
        add("        enabled.set(${client.consumerEnabled})")
        add("    }")
        client.topicParameterProperties.forEach { (parameter, property) ->
            add("    topicParameterProperties.put(${parameter.kotlinStringLiteral()}, ${property.kotlinStringLiteral()})")
        }
        if (client.clientContractValidationAnnotation != null || client.payloadParameterValidationAnnotation != null) {
            add("    validationAnnotations {")
            client.clientContractValidationAnnotation?.let { add("        clientContract.set(${it.kotlinStringLiteral()})") }
            client.payloadParameterValidationAnnotation?.let { add("        payloadParameter.set(${it.kotlinStringLiteral()})") }
            add("    }")
        }
        add("}")
    }.joinToString("\n") { "    $it" }

    private companion object {
        const val GENERATOR_GROUP = "dev.banking.asyncapi.generator"
    }
}
