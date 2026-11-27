package dev.banking.asyncapi.generator.gradle.plugin.extensions

import dev.banking.asyncapi.generator.core.GeneratorSettings
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import javax.inject.Inject

/**
 * Gradle configuration surface for the AsyncAPI generator.
 *
 * @param objects creates the executions
 */
abstract class AsyncApiExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        /** Client settings shared by every execution. */
        @get:Nested
        abstract val clientConfig: AsyncApiClientConfiguration

        /**
         * A local `protoc` executable for native Protobuf models. By default `com.google.protobuf:protoc`
         * is resolved from the project's repositories, and only for executions that generate them.
         */
        abstract val protocExecutable: RegularFileProperty

        /** Configuration warning ids silenced for every execution, for example `unused-client-config`. */
        abstract val suppressedWarnings: ListProperty<String>

        /** The generation requests; each one becomes a task. */
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
 * @param executionName the name of the request, which also names its task
 */
abstract class AsyncApiExecution
    @Inject
    constructor(
        private val executionName: String,
    ) : Named {
        override fun getName(): String = executionName

        abstract val generatorName: Property<String>
        abstract val inputSpec: RegularFileProperty
        abstract val outputFile: RegularFileProperty
        abstract val outputDirectory: DirectoryProperty
        abstract val modelPackage: Property<String>
        abstract val clientPackage: Property<String>
        abstract val schemaPackage: Property<String>

        /** Configuration warning ids silenced for this execution, in addition to the top-level ones. */
        abstract val suppressedWarnings: ListProperty<String>

        /** Payload model settings of this execution. */
        @get:Nested
        abstract val modelConfig: AsyncApiModelConfiguration

        fun modelConfig(action: Action<AsyncApiModelConfiguration>) {
            action.execute(modelConfig)
        }
    }

/** Gradle model generation configuration. */
abstract class AsyncApiModelConfiguration {
    @get:Input
    @get:Optional
    abstract val modelAnnotation: Property<String>

    @get:Input
    @get:Optional
    abstract val modelType: Property<String>

    /** Takes every value from [source] lazily, so later changes to [source] still apply. */
    internal fun from(source: AsyncApiModelConfiguration) {
        modelAnnotation.set(source.modelAnnotation)
        modelType.set(source.modelType)
    }

    /** The settings, or `null` when nothing is set. */
    internal fun toSettings(): GeneratorSettings.ModelConfig? =
        GeneratorSettings
            .ModelConfig(modelAnnotation = modelAnnotation.orNull, modelType = modelType.orNull)
            .takeIf { modelAnnotation.isPresent || modelType.isPresent }
}

/**
 * Shared Gradle client generation configuration. It counts as configured once any of its properties is
 * set; an empty `clientConfig {}` block configures nothing.
 */
abstract class AsyncApiClientConfiguration {
    @get:Input
    @get:Optional
    abstract val clientType: Property<String>

    @get:Input
    @get:Optional
    abstract val clientContract: Property<String>

    @get:Input
    @get:Optional
    abstract val topicParameterProperties: MapProperty<String, String>

    @get:Nested
    abstract val validationAnnotations: AsyncApiValidationAnnotationsConfiguration

    @get:Nested
    abstract val producer: AsyncApiProducerConfiguration

    @get:Nested
    abstract val consumer: AsyncApiConsumerConfiguration

    fun validationAnnotations(action: Action<AsyncApiValidationAnnotationsConfiguration>) {
        action.execute(validationAnnotations)
    }

    fun producer(action: Action<AsyncApiProducerConfiguration>) {
        action.execute(producer)
    }

    fun consumer(action: Action<AsyncApiConsumerConfiguration>) {
        action.execute(consumer)
    }

    /** Takes every value from [source] lazily, so later changes to [source] still apply. */
    internal fun from(source: AsyncApiClientConfiguration) {
        clientType.set(source.clientType)
        clientContract.set(source.clientContract)
        topicParameterProperties.set(source.topicParameterProperties)
        validationAnnotations.clientContract.set(source.validationAnnotations.clientContract)
        validationAnnotations.payloadParameter.set(source.validationAnnotations.payloadParameter)
        producer.enabled.set(source.producer.enabled)
        producer.additionalPayloadTypes.set(source.producer.additionalPayloadTypes)
        consumer.enabled.set(source.consumer.enabled)
    }

    /** The settings, or `null` when nothing is set. */
    internal fun toSettings(): GeneratorSettings.ClientConfig? {
        val producerSettings = producer.toSettings()
        val consumerSettings = consumer.enabled.orNull?.let(GeneratorSettings::ConsumerConfig)
        val annotationSettings = validationAnnotations.toSettings()
        val configured =
            clientType.isPresent ||
                clientContract.isPresent ||
                topicParameterProperties.orNull.orEmpty().isNotEmpty() ||
                listOfNotNull(producerSettings, consumerSettings, annotationSettings).isNotEmpty()
        return GeneratorSettings
            .ClientConfig(
                clientType = clientType.orNull,
                clientContract = clientContract.orNull,
                producer = producerSettings,
                consumer = consumerSettings,
                topicParameterProperties = topicParameterProperties.orNull.orEmpty(),
                validationAnnotations = annotationSettings,
            ).takeIf { configured }
    }
}

/** Validation annotations applied to generated client contracts. */
abstract class AsyncApiValidationAnnotationsConfiguration {
    @get:Input
    @get:Optional
    abstract val clientContract: Property<String>

    @get:Input
    @get:Optional
    abstract val payloadParameter: Property<String>

    internal fun toSettings(): GeneratorSettings.ValidationAnnotationsConfig? =
        GeneratorSettings
            .ValidationAnnotationsConfig(
                clientContract = clientContract.orNull,
                payloadParameter = payloadParameter.orNull,
            ).takeIf { clientContract.isPresent || payloadParameter.isPresent }
}

/** Gradle producer generation configuration. */
abstract class AsyncApiProducerConfiguration {
    @get:Input
    @get:Optional
    abstract val enabled: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val additionalPayloadTypes: ListProperty<String>

    internal fun toSettings(): GeneratorSettings.ProducerConfig? {
        val payloadTypes = additionalPayloadTypes.orNull.orEmpty()
        return GeneratorSettings
            .ProducerConfig(enabled = enabled.orNull, additionalPayloadTypes = payloadTypes.ifEmpty { null })
            .takeIf { enabled.isPresent || payloadTypes.isNotEmpty() }
    }
}

/** Gradle consumer generation configuration. */
abstract class AsyncApiConsumerConfiguration {
    @get:Input
    @get:Optional
    abstract val enabled: Property<Boolean>
}
