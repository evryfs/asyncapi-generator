package dev.banking.asyncapi.generator.gradle.plugin.extensions

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Gradle configuration surface for the AsyncAPI generator.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiExtension @Inject constructor(objects: ObjectFactory) {
    val clientConfig: AsyncApiClientConfiguration =
        objects.newInstance(AsyncApiClientConfiguration::class.java)

    val executions: NamedDomainObjectContainer<AsyncApiExecution> =
        objects.domainObjectContainer(AsyncApiExecution::class.java) { executionName ->
            objects.newInstance(AsyncApiExecution::class.java, executionName)
        }

    fun clientConfig(action: Action<AsyncApiClientConfiguration>) {
        action.execute(clientConfig)
    }

    fun executions(action: Action<NamedDomainObjectContainer<AsyncApiExecution>>) {
        action.execute(executions)
    }
}

/**
 * One named AsyncAPI generation request.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiExecution @Inject constructor(
    private val executionName: String,
    objects: ObjectFactory,
) : Named {
    override fun getName(): String = executionName

    val generatorName: Property<String> = objects.property(String::class.java)
    val inputSpec: RegularFileProperty = objects.fileProperty()
    val outputFile: RegularFileProperty = objects.fileProperty()
    val outputDirectory: DirectoryProperty = objects.directoryProperty()
    val modelPackage: Property<String> = objects.property(String::class.java)
    val clientPackage: Property<String> = objects.property(String::class.java)
    val schemaPackage: Property<String> = objects.property(String::class.java)
    val modelConfig: AsyncApiModelConfiguration =
        objects.newInstance(AsyncApiModelConfiguration::class.java)

    fun modelConfig(action: Action<AsyncApiModelConfiguration>) {
        action.execute(modelConfig)
    }
}

/**
 * Gradle model generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiModelConfiguration @Inject constructor(objects: ObjectFactory) {
    val modelAnnotation: Property<String> = objects.property(String::class.java)
    val modelType: Property<String> = objects.property(String::class.java)
}

/**
 * Shared Gradle client generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiClientConfiguration @Inject constructor(objects: ObjectFactory) {
    val clientType: Property<String> = objects.property(String::class.java)
    val clientContract: Property<String> = objects.property(String::class.java)
    val topicParameterProperties: MapProperty<String, String> =
        objects.mapProperty(String::class.java, String::class.java)
    val validationAnnotations: AsyncApiValidationAnnotationsConfiguration =
        objects.newInstance(AsyncApiValidationAnnotationsConfiguration::class.java)
    val producer: AsyncApiProducerConfiguration =
        objects.newInstance(AsyncApiProducerConfiguration::class.java)
    val consumer: AsyncApiConsumerConfiguration =
        objects.newInstance(AsyncApiConsumerConfiguration::class.java)

    fun validationAnnotations(action: Action<AsyncApiValidationAnnotationsConfiguration>) {
        action.execute(validationAnnotations)
    }

    fun producer(action: Action<AsyncApiProducerConfiguration>) {
        action.execute(producer)
    }

    fun consumer(action: Action<AsyncApiConsumerConfiguration>) {
        action.execute(consumer)
    }
}

/**
 * Validation annotations applied to generated client contracts.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiValidationAnnotationsConfiguration @Inject constructor(objects: ObjectFactory) {
    val clientContract: Property<String> = objects.property(String::class.java)
    val payloadParameter: Property<String> = objects.property(String::class.java)
}

/**
 * Gradle producer generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiProducerConfiguration @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.javaObjectType)
}

/**
 * Gradle consumer generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiConsumerConfiguration @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.javaObjectType)
}
