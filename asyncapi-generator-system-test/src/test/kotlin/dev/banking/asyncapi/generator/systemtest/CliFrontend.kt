package dev.banking.asyncapi.generator.systemtest

import java.io.File

/**
 * Runs the staged, shaded CLI jar the way a user would: `java -jar`.
 *
 * @param environment Build-provided settings.
 * @param runner Process runner shared by all frontends.
 */
internal class CliFrontend(
    private val environment: SystemTestEnvironment,
    private val runner: ProcessRunner,
) {

    /** Generates [scenario] into a fresh directory and returns that directory. */
    fun generate(scenario: Scenario, workDirectory: File): File {
        val output = workDirectory.resolve("output").apply { deleteRecursively() }
        val command = listOf(javaExecutable(), "-jar", environment.cliJar.path) +
            arguments(scenario.config, scenario.specDirectory(environment), output)
        runner.runSuccessfully(command, workDirectory.apply { mkdirs() }, workDirectory.resolve("cli.log"))
        return output
    }

    private fun arguments(config: GenerationConfig, specDirectory: File, output: File): List<String> = buildList {
        add("--input-spec"); add(specDirectory.resolve(config.inputSpec).path)
        add("--generator-name"); add(config.generatorName)
        add("--output-directory"); add(output.path)
        config.modelPackage?.let { add("--model-package"); add(it) }
        config.clientPackage?.let { add("--client-package"); add(it) }
        config.schemaPackage?.let { add("--schema-package"); add(it) }
        config.modelType?.let { add("--model-type"); add(it) }
        config.modelAnnotation?.let { add("--model-annotation"); add(it) }
        config.client?.let { client ->
            add("--client-type"); add(client.clientType)
            add("--client-contract"); add(client.clientContract)
            add(if (client.producerEnabled) "--generate-producer" else "--no-generate-producer")
            client.producerAdditionalPayloadTypes.forEach { add("--producer-additional-payload-type"); add(it) }
            add(if (client.consumerEnabled) "--generate-consumer" else "--no-generate-consumer")
            client.topicParameterProperties.forEach { (parameter, property) ->
                add("--topic-parameter-property"); add("$parameter=$property")
            }
            client.clientContractValidationAnnotation?.let {
                add("--client-contract-validation-annotation"); add(it)
            }
            client.payloadParameterValidationAnnotation?.let {
                add("--payload-parameter-validation-annotation"); add(it)
            }
        }
    }

    private fun javaExecutable(): String =
        File(System.getProperty("java.home")).resolve("bin/java").path
}
