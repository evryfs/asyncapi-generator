package dev.banking.asyncapi.generator.systemtest

import java.io.File

/**
 * Runs the staged, shaded CLI jar the way a user would: `java -jar`, once per execution.
 *
 * Each invocation runs in a project directory laid out like the consumer
 * projects, so relative paths resolve the same way for every frontend.
 *
 * @param environment Build-provided settings.
 * @param runner Process runner shared by all frontends.
 */
internal class CliFrontend(
    private val environment: SystemTestEnvironment,
    private val runner: ProcessRunner,
) {

    /** Runs every execution of [scenario] in a fresh [projectDirectory]; stops at the first failure. */
    fun run(scenario: Scenario, projectDirectory: File): FrontendRun {
        ConsumerProject.prepare(scenario, environment, projectDirectory)
        var result: ProcessResult? = null
        for (execution in scenario.executions) {
            result = generate(execution, scenario.client, projectDirectory)
            if (result.exitCode != 0) break
        }
        return FrontendRun.of(scenario, projectDirectory, checkNotNull(result)) { DEFAULT_OUTPUT_DIRECTORY }
    }

    /** Runs one execution in an existing [projectDirectory] without checking the exit code. */
    fun generate(execution: Execution, client: ClientConfig?, projectDirectory: File): ProcessResult {
        val command = listOf(javaExecutable(), "-jar", environment.cliJar.path) + arguments(execution, client)
        return runner.run(command, projectDirectory, projectDirectory.resolve("logs/${execution.id}.log"))
    }

    private fun arguments(execution: Execution, client: ClientConfig?): List<String> = buildList {
        add("--input-spec"); add(ConsumerProject.inputSpecPath(execution))
        add("--generator-name"); add(execution.generatorName)
        execution.outputDirectory?.let { add("--output-directory"); add(it) }
        execution.outputFile?.let { add("--output-file"); add(it) }
        execution.modelPackage?.let { add("--model-package"); add(it) }
        execution.clientPackage?.let { add("--client-package"); add(it) }
        execution.schemaPackage?.let { add("--schema-package"); add(it) }
        execution.modelType?.let { add("--model-type"); add(it) }
        execution.modelAnnotation?.let { add("--model-annotation"); add(it) }
        client?.takeIf { execution.clientPackage != null }?.let { addAll(clientArguments(it)) }
    }

    private fun clientArguments(client: ClientConfig): List<String> = buildList {
        add("--client-type"); add(client.clientType)
        add("--client-contract"); add(client.clientContract)
        add(if (client.producerEnabled) "--generate-producer" else "--no-generate-producer")
        client.producerAdditionalPayloadTypes.forEach { add("--producer-additional-payload-type"); add(it) }
        add(if (client.consumerEnabled) "--generate-consumer" else "--no-generate-consumer")
        client.topicParameterProperties.forEach { (parameter, property) ->
            add("--topic-parameter-property"); add("$parameter=$property")
        }
        client.clientContractValidationAnnotation?.let { add("--client-contract-validation-annotation"); add(it) }
        client.payloadParameterValidationAnnotation?.let { add("--payload-parameter-validation-annotation"); add(it) }
    }

    private fun javaExecutable(): String =
        File(System.getProperty("java.home")).resolve("bin/java").path

    private companion object {
        /** The CLI's documented default for `--output-directory`. */
        const val DEFAULT_OUTPUT_DIRECTORY = "generated/asyncapi"
    }
}
