package dev.banking.asyncapi.generator.systemtest

import java.io.File

/**
 * Builds a real Maven consumer project that applies the staged Maven plugin.
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
        ConsumerProject.prepare(scenario, environment, projectDirectory)
        projectDirectory.resolve("pom.xml").writeText(pom(scenario))
        projectDirectory.resolve("settings.xml").writeText(settings())

        val command = listOf(
            environment.mavenHome.resolve("bin/mvn").path,
            "--batch-mode",
            "--no-transfer-progress",
            "--settings", "settings.xml",
            "-Dmaven.repo.local=${environment.repository.path}",
            "verify",
        )
        val result = runner.run(
            command,
            projectDirectory,
            projectDirectory.resolve("build.log"),
            environment.buildToolEnvironment,
        )
        return FrontendRun.of(scenario, projectDirectory, result) { DEFAULT_OUTPUT_DIRECTORY }
    }

    private fun pom(scenario: Scenario): String = """
        |<?xml version="1.0" encoding="UTF-8"?>
        |<project xmlns="http://maven.apache.org/POM/4.0.0">
        |    <modelVersion>4.0.0</modelVersion>
        |    <groupId>dev.banking.asyncapi.generator.systemtest</groupId>
        |    <artifactId>${ConsumerProject.projectName(scenario)}</artifactId>
        |    <version>1.0-SNAPSHOT</version>
        |    <properties>
        |        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        |        <maven.compiler.release>21</maven.compiler.release>
        |    </properties>
        |    <dependencies>
        |${dependencyElements(ConsumerDependencies(environment.libraries, scenario.libraries))}
        |    </dependencies>
        |    <build>
        |        <testSourceDirectory>src/test/kotlin</testSourceDirectory>
        |        <plugins>
        |            <plugin>
        |                <groupId>dev.banking.asyncapi.generator</groupId>
        |                <artifactId>asyncapi-generator-maven-plugin</artifactId>
        |                <version>${environment.version}</version>
        |                <executions>
        |${scenario.executions.joinToString("\n") { execution(it, scenario.client) }}
        |                </executions>
        |            </plugin>
        |            <plugin>
        |                <groupId>org.jetbrains.kotlin</groupId>
        |                <artifactId>kotlin-maven-plugin</artifactId>
        |                <version>${environment.libraries.kotlin}</version>
        |                <configuration>
        |                    <jvmTarget>21</jvmTarget>
        |                </configuration>
        |                <executions>
        |                    <execution>
        |                        <id>compile</id>
        |                        <goals>
        |                            <goal>compile</goal>
        |                        </goals>
        |                    </execution>
        |                    <execution>
        |                        <id>test-compile</id>
        |                        <goals>
        |                            <goal>test-compile</goal>
        |                        </goals>
        |                    </execution>
        |                </executions>
        |            </plugin>
        |        </plugins>
        |    </build>
        |</project>
        |""".trimMargin()

    private fun dependencyElements(dependencies: ConsumerDependencies): String =
        (dependencies.implementation.map { it to null } + dependencies.test.map { it to "test" })
            .joinToString("\n") { (dependency, scope) ->
                buildString {
                    append("        <dependency>")
                    append("<groupId>${dependency.group}</groupId>")
                    append("<artifactId>${dependency.artifact}</artifactId>")
                    append("<version>${dependency.version}</version>")
                    scope?.let { append("<scope>$it</scope>") }
                    append("</dependency>")
                }
            }

    private fun execution(execution: Execution, client: ClientConfig?): String = buildList {
        add("<execution>")
        add("    <id>${execution.id.xmlEscaped()}</id>")
        add("    <goals><goal>generate</goal></goals>")
        add("    <configuration>")
        executionConfiguration(execution).forEach { add("        $it") }
        client?.takeIf { execution.clientPackage != null }?.let { clientConfiguration(it).forEach { line -> add("        $line") } }
        add("    </configuration>")
        add("</execution>")
    }.joinToString("\n") { "                    $it" }

    private fun executionConfiguration(execution: Execution): List<String> = buildList {
        add(element("inputSpec", "\${project.basedir}/${ConsumerProject.inputSpecPath(execution)}"))
        add(element("generatorName", execution.generatorName))
        execution.outputDirectory?.let { add(element("outputDirectory", it)) }
        execution.outputFile?.let { add(element("outputFile", it)) }
        execution.modelPackage?.let { add(element("modelPackage", it)) }
        execution.clientPackage?.let { add(element("clientPackage", it)) }
        execution.schemaPackage?.let { add(element("schemaPackage", it)) }
        if (execution.modelType != null || execution.modelAnnotation != null) {
            add("<modelConfig>")
            execution.modelType?.let { add(element("modelType", it)) }
            execution.modelAnnotation?.let { add(element("modelAnnotation", it)) }
            add("</modelConfig>")
        }
    }

    private fun clientConfiguration(client: ClientConfig): List<String> = buildList {
        add("<clientConfig>")
        add(element("clientType", client.clientType))
        add(element("clientContract", client.clientContract))
        add("<producer>")
        add(element("enabled", client.producerEnabled.toString()))
        if (client.producerAdditionalPayloadTypes.isNotEmpty()) {
            add("<additionalPayloadTypes>")
            client.producerAdditionalPayloadTypes.forEach { add(element("additionalPayloadType", it)) }
            add("</additionalPayloadTypes>")
        }
        add("</producer>")
        add("<consumer>")
        add(element("enabled", client.consumerEnabled.toString()))
        add("</consumer>")
        if (client.topicParameterProperties.isNotEmpty()) {
            add("<topicParameterProperties>")
            client.topicParameterProperties.forEach { (parameter, property) -> add(element(parameter, property)) }
            add("</topicParameterProperties>")
        }
        if (client.clientContractValidationAnnotation != null || client.payloadParameterValidationAnnotation != null) {
            add("<validationAnnotations>")
            client.clientContractValidationAnnotation?.let { add(element("clientContract", it)) }
            client.payloadParameterValidationAnnotation?.let { add(element("payloadParameter", it)) }
            add("</validationAnnotations>")
        }
        add("</clientConfig>")
    }

    private fun element(name: String, value: String): String = "<$name>${value.xmlEscaped()}</$name>"

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
