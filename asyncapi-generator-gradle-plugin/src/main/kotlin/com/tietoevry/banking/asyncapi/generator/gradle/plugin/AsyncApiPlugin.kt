package dev.banking.asyncapi.generator.gradle.plugin

import dev.banking.asyncapi.generator.gradle.plugin.extensions.AsyncApiExtension
import dev.banking.asyncapi.generator.gradle.plugin.tasks.GenerateAsyncApiTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.register

class AsyncApiPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "asyncapiGenerate",
            AsyncApiExtension::class.java,
            project.objects
        )

        val task = project.tasks.register<GenerateAsyncApiTask>("generateAsyncApi") {
            group = "code generation"
            description = "Generates source code and clients from an AsyncAPI specification"

            inputFile.convention(project.layout.projectDirectory.file("src/main/resources/asyncapi.yaml"))
            outputDir.convention(project.layout.buildDirectory.dir("generated/asyncapi"))
            modelPackage.convention("com.example.asyncapi.model")
            generatorName.convention("kotlin")

            inputFile.set(extension.inputFile)
            outputFile.set(extension.outputFile)
            outputDir.set(extension.outputDir)
            modelPackage.set(extension.modelPackage)
            clientPackage.set(extension.clientPackage)
            schemaPackage.set(extension.schemaPackage)
            generatorName.set(extension.generatorName)
            configuration.set(extension.configuration)
            experimental.set(extension.experimental)
        }

        // Register Source Set (Standard Gradle way to make generated code usable)
        project.afterEvaluate {
            val javaPluginExtension = project.extensions.findByType(JavaPluginExtension::class.java)
            if (javaPluginExtension != null) {
                val sourceSet = javaPluginExtension.sourceSets.getByName("main")

                sourceSet.java.srcDir(task.map { it.outputDir.dir("src/main/kotlin") })
                sourceSet.java.srcDir(task.map { it.outputDir.dir("src/main/java") })
            }
        }
    }
}

