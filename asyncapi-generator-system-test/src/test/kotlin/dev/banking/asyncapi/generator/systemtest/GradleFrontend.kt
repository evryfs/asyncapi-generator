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
    private val dependencies = ConsumerDependencies(environment.kotlinVersion)

    /** Generates, compiles, and runs the checks for [scenario]; returns the generated source directory. */
    fun build(scenario: Scenario, projectDirectory: File): File {
        ConsumerProject.prepare(scenario, environment, projectDirectory)
        installWrapper(projectDirectory)
        projectDirectory.resolve("settings.gradle.kts").writeText(settings(scenario))
        projectDirectory.resolve("build.gradle.kts").writeText(buildScript(scenario.config))
        projectDirectory.resolve("gradle.properties").writeText(
            "org.gradle.daemon.idletimeout=600000\norg.gradle.console=plain\n",
        )

        val command = listOf(projectDirectory.resolve("gradlew").path, "build", "--stacktrace")
        runner.runSuccessfully(
            command,
            projectDirectory,
            projectDirectory.resolve("build.log"),
            environment.buildToolEnvironment,
        )
        return projectDirectory.resolve("build/generated/asyncapi/$EXECUTION_NAME")
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

    private fun buildScript(config: GenerationConfig): String = """
        |plugins {
        |    kotlin("jvm") version "${environment.kotlinVersion}"
        |    id("$GENERATOR_GROUP") version "${environment.version}"
        |}
        |
        |dependencies {
        |    implementation(platform("org.springframework.boot:spring-boot-dependencies:${environment.springBootVersion}"))
        |${dependencyDeclarations()}
        |    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
        |${config.client?.let(::clientConfiguration).orEmpty()}
        |    executions {
        |        register("$EXECUTION_NAME") {
        |${executionConfiguration(config)}
        |        }
        |    }
        |}
        |""".trimMargin()

    private fun dependencyDeclarations(): String =
        (dependencies.implementation.map { "implementation" to it } + dependencies.test.map { "testImplementation" to it })
            .joinToString("\n") { (configuration, dependency) ->
                val coordinates = listOfNotNull(dependency.group, dependency.artifact, dependency.version).joinToString(":")
                "    $configuration(\"$coordinates\")"
            }

    private fun executionConfiguration(config: GenerationConfig): String = buildList {
        add("generatorName.set(${config.generatorName.kotlinStringLiteral()})")
        add("inputSpec.set(file(${ConsumerProject.inputSpecPath(config).kotlinStringLiteral()}))")
        config.modelPackage?.let { add("modelPackage.set(${it.kotlinStringLiteral()})") }
        config.clientPackage?.let { add("clientPackage.set(${it.kotlinStringLiteral()})") }
        config.schemaPackage?.let { add("schemaPackage.set(${it.kotlinStringLiteral()})") }
        if (config.modelType != null || config.modelAnnotation != null) {
            add("modelConfig {")
            config.modelType?.let { add("    modelType.set(${it.kotlinStringLiteral()})") }
            config.modelAnnotation?.let { add("    modelAnnotation.set(${it.kotlinStringLiteral()})") }
            add("}")
        }
    }.joinToString("\n") { "            $it" }

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
        const val EXECUTION_NAME = "asyncapi"
    }
}
