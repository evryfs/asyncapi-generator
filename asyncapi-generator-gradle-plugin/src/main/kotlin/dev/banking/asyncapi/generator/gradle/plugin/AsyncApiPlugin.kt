package dev.banking.asyncapi.generator.gradle.plugin

import dev.banking.asyncapi.generator.core.ProtocProvider
import dev.banking.asyncapi.generator.gradle.plugin.extensions.AsyncApiExecution
import dev.banking.asyncapi.generator.gradle.plugin.extensions.AsyncApiExtension
import dev.banking.asyncapi.generator.gradle.plugin.tasks.GenerateAsyncApiTask
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.kotlin.dsl.register

/**
 * Registers the AsyncAPI generator extension and one task for each named execution.
 */
class AsyncApiPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("asyncApiGenerator", AsyncApiExtension::class.java, project.objects)
        val aggregateTask =
            project.tasks.register("generateAsyncApi") {
                group = "asyncapi"
                description = "Generates every configured AsyncAPI execution."
            }
        val outputRegistrar = GradleGeneratedOutputRegistrar(project)
        val protocConfiguration = protocConfiguration(project)

        extension.executions.all {
            outputDirectory.convention(project.layout.buildDirectory.dir("generated/asyncapi/$name"))
            val executionTask = registerTask(project, extension, this, protocConfiguration)
            aggregateTask.configure { dependsOn(executionTask) }
            outputRegistrar.register(executionTask)
        }
    }

    /** The `protoc` executable, resolved from the project's repositories only by executions that run it. */
    private fun protocConfiguration(project: Project): NamedDomainObjectProvider<Configuration> =
        project.configurations.register("asyncApiProtoc") {
            isCanBeConsumed = false
            description = "The protoc compiler for native Protobuf models."
            defaultDependencies {
                add(
                    project.dependencies.create(
                        "com.google.protobuf:protoc:${ProtocProvider.DEFAULT_VERSION}:" +
                            "${ProtocProvider.platformClassifier()}@exe",
                    ),
                )
            }
        }

    private fun registerTask(
        project: Project,
        extension: AsyncApiExtension,
        execution: AsyncApiExecution,
        protocConfiguration: NamedDomainObjectProvider<Configuration>,
    ) = project.tasks.register<GenerateAsyncApiTask>(executionTaskName(execution.name)) {
        group = "asyncapi"
        description = "Generates the '${execution.name}' AsyncAPI execution."

        inputSpec.set(execution.inputSpec)
        inputFiles.from(
            project.providers.of(AsyncApiInputFilesValueSource::class.java) {
                parameters.inputSpec.set(execution.inputSpec)
            },
        )
        outputFile.set(execution.outputFile)
        outputDirectory.set(execution.outputDirectory)
        generatorName.set(execution.generatorName)
        modelPackage.set(execution.modelPackage)
        clientPackage.set(execution.clientPackage)
        schemaPackage.set(execution.schemaPackage)
        modelConfig.from(execution.modelConfig)
        clientConfig.from(extension.clientConfig)
        suppressedWarnings.addAll(extension.suppressedWarnings)
        suppressedWarnings.addAll(execution.suppressedWarnings)
        outputManifest.set(project.layout.buildDirectory.file("asyncapi-generator/output-manifest.txt"))
        executionName.set(execution.name)
        protoc.from(protocFiles(project, extension, protocConfiguration))
    }

    /** `protoc` for executions that run it: the configured executable, or the resolved `asyncApiProtoc`. */
    private fun GenerateAsyncApiTask.protocFiles(
        project: Project,
        extension: AsyncApiExtension,
        protocConfiguration: NamedDomainObjectProvider<Configuration>,
    ) = project.provider {
        when {
            generation()?.requiresProtoc != true -> emptyList<Any>()
            extension.protocExecutable.isPresent -> listOf(extension.protocExecutable)
            else -> listOf(protocConfiguration)
        }
    }

    private fun executionTaskName(executionName: String): String =
        "generate${executionName.replaceFirstChar(Char::uppercaseChar)}AsyncApi"
}
