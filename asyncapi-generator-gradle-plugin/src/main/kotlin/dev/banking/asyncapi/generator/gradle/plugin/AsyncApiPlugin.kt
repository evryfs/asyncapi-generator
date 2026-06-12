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

        extension.inputFile.convention(project.layout.projectDirectory.file("src/main/resources/asyncapi.yaml"))
        extension.codegenOutputDirectory.convention(project.layout.buildDirectory.dir("generated/asyncapi"))
        extension.resourceOutputDirectory.convention(project.layout.buildDirectory.dir("generated-resources/asyncapi"))
        extension.generatorName.convention("kotlin")

        val task = project.tasks.register<GenerateAsyncApiTask>("generateAsyncApi") {

            inputFile.set(extension.inputFile)
            outputFile.set(extension.outputFile)
            codegenOutputDirectory.set(extension.codegenOutputDirectory)
            resourceOutputDirectory.set(extension.resourceOutputDirectory)
            generatorName.set(extension.generatorName)

            modelsEnabled.set(extension.models.enabled)
            modelsPackageName.set(extension.models.packageName)
            modelsAnnotation.set(extension.models.annotation)
            modelsJavaModelType.set(extension.models.javaModelType)

            avroProjectionEnabled.set(extension.schemas.avroProjection.enabled)
            avroProjectionPackageName.set(extension.schemas.avroProjection.packageName)
            nativeAvroEnabled.set(extension.schemas.nativeAvro.enabled)
            nativeAvroGenerateSpecificRecords.set(extension.schemas.nativeAvro.generateSpecificRecords)
            nativeProtobufEnabled.set(extension.schemas.nativeProtobuf.enabled)

            springKafkaEnabled.set(extension.clients.springKafka.enabled)
            springKafkaPackageName.set(extension.clients.springKafka.packageName)
            springKafkaModelPackageName.set(extension.clients.springKafka.modelPackageName)
            springKafkaMode.set(extension.clients.springKafka.mode)
            springKafkaTopicPropertyPrefix.set(extension.clients.springKafka.topicPropertyPrefix)

            quarkusKafkaEnabled.set(extension.clients.quarkusKafka.enabled)
            quarkusKafkaPackageName.set(extension.clients.quarkusKafka.packageName)
            quarkusKafkaModelPackageName.set(extension.clients.quarkusKafka.modelPackageName)
        }

        // Register Source Set (Standard Gradle way to make generated code usable)
        project.afterEvaluate {
            val javaPluginExtension = project.extensions.findByType(JavaPluginExtension::class.java)
            if (javaPluginExtension != null) {
                val sourceSet = javaPluginExtension.sourceSets.getByName("main")

                sourceSet.java.srcDir(task.map { it.codegenOutputDirectory.dir("src/main/kotlin") })
                sourceSet.java.srcDir(task.map { it.codegenOutputDirectory.dir("src/main/java") })
            }
        }
    }
}
