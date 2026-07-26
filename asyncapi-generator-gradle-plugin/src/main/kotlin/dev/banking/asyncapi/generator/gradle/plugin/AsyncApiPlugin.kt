package dev.banking.asyncapi.generator.gradle.plugin

import dev.banking.asyncapi.generator.gradle.plugin.extensions.AsyncApiExtension
import dev.banking.asyncapi.generator.gradle.plugin.tasks.GenerateAsyncApiTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

/**
 * Registers the AsyncAPI generator extension and one task for each named execution.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
class AsyncApiPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create(
                "asyncApiGenerator",
                AsyncApiExtension::class.java,
                project.objects,
            )
        val aggregateTask =
            project.tasks.register("generateAsyncApi") {
                group = "asyncapi"
                description = "Generates every configured AsyncAPI execution."
            }
        val outputRegistrar = GradleGeneratedOutputRegistrar(project)

        extension.executions.all {
            val execution = this
            outputDirectory.convention(
                project.layout.buildDirectory.dir("generated/asyncapi/$name"),
            )

            val executionTask =
                project.tasks.register<GenerateAsyncApiTask>(executionTaskName(name)) {
                    group = "asyncapi"
                    description = "Generates the '$name' AsyncAPI execution."

                    inputSpec.set(execution.inputSpec)
                    outputFile.set(execution.outputFile)
                    outputDirectory.set(execution.outputDirectory)
                    generatorName.set(execution.generatorName)
                    modelPackage.set(execution.modelPackage)
                    clientPackage.set(execution.clientPackage)
                    schemaPackage.set(execution.schemaPackage)
                    modelAnnotation.set(execution.modelConfig.modelAnnotation)
                    modelType.set(execution.modelConfig.modelType)

                    clientType.set(extension.clientConfig.clientType)
                    clientContract.set(extension.clientConfig.clientContract)
                    producerEnabled.set(extension.clientConfig.producer.enabled)
                    consumerEnabled.set(extension.clientConfig.consumer.enabled)
                    topicParameterProperties.set(extension.clientConfig.topicParameterProperties)
                    clientContractValidationAnnotation.set(
                        extension.clientConfig.validationAnnotations.clientContract,
                    )
                    payloadParameterValidationAnnotation.set(
                        extension.clientConfig.validationAnnotations.payloadParameter,
                    )
                }

            aggregateTask.configure {
                dependsOn(executionTask)
            }
            outputRegistrar.register(executionTask)
        }
    }

    private fun executionTaskName(executionName: String): String =
        "generate${executionName.replaceFirstChar(Char::uppercaseChar)}AsyncApi"
}
