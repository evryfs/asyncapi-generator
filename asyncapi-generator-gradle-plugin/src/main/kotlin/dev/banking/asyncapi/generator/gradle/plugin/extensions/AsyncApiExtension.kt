package dev.banking.asyncapi.generator.gradle.plugin.extensions

import org.gradle.api.Action
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Gradle configuration surface for the AsyncAPI generator.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiExtension @Inject constructor(objects: ObjectFactory) {
    val inputFile: RegularFileProperty = objects.fileProperty()
    val outputFile: RegularFileProperty = objects.fileProperty()
    val codegenOutputDirectory: DirectoryProperty = objects.directoryProperty()
    val resourceOutputDirectory: DirectoryProperty = objects.directoryProperty()

    val generatorName: Property<String> = objects.property(String::class.java)

    val models: AsyncApiModelsExtension = objects.newInstance(AsyncApiModelsExtension::class.java)
    val schemas: AsyncApiSchemasExtension = objects.newInstance(AsyncApiSchemasExtension::class.java)
    val clients: AsyncApiClientsExtension = objects.newInstance(AsyncApiClientsExtension::class.java)

    fun models(action: Action<AsyncApiModelsExtension>) {
        action.execute(models)
    }

    fun schemas(action: Action<AsyncApiSchemasExtension>) {
        action.execute(schemas)
    }

    fun clients(action: Action<AsyncApiClientsExtension>) {
        action.execute(clients)
    }
}

/**
 * Gradle model generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiModelsExtension @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.javaObjectType)
    val packageName: Property<String> = objects.property(String::class.java)
    val annotation: Property<String> = objects.property(String::class.java)
    val javaModelType: Property<String> = objects.property(String::class.java)
}

/**
 * Gradle schema generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiSchemasExtension @Inject constructor(objects: ObjectFactory) {
    val avroProjection: AsyncApiAvroProjectionExtension =
        objects.newInstance(AsyncApiAvroProjectionExtension::class.java)
    val nativeAvro: AsyncApiNativeAvroExtension =
        objects.newInstance(AsyncApiNativeAvroExtension::class.java)
    val nativeProtobuf: AsyncApiNativeProtobufExtension =
        objects.newInstance(AsyncApiNativeProtobufExtension::class.java)

    fun avroProjection(action: Action<AsyncApiAvroProjectionExtension>) {
        action.execute(avroProjection)
    }

    fun nativeAvro(action: Action<AsyncApiNativeAvroExtension>) {
        action.execute(nativeAvro)
    }

    fun nativeProtobuf(action: Action<AsyncApiNativeProtobufExtension>) {
        action.execute(nativeProtobuf)
    }
}

/**
 * Gradle Avro projection configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiAvroProjectionExtension @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.javaObjectType)
    val packageName: Property<String> = objects.property(String::class.java)
}

/**
 * Gradle native Avro schema generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiNativeAvroExtension @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.javaObjectType)
    val generateSpecificRecords: Property<Boolean> = objects.property(Boolean::class.javaObjectType)
}

/**
 * Gradle native Protobuf schema generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiNativeProtobufExtension @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.javaObjectType)
    val generateJavaMessageTypes: Property<Boolean> = objects.property(Boolean::class.javaObjectType)
}

/**
 * Gradle client generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiClientsExtension @Inject constructor(objects: ObjectFactory) {
    val kafka: AsyncApiKafkaExtension =
        objects.newInstance(AsyncApiKafkaExtension::class.java)
    val quarkusKafka: AsyncApiQuarkusKafkaExtension =
        objects.newInstance(AsyncApiQuarkusKafkaExtension::class.java)

    fun kafka(action: Action<AsyncApiKafkaExtension>) {
        action.execute(kafka)
    }

    fun quarkusKafka(action: Action<AsyncApiQuarkusKafkaExtension>) {
        action.execute(quarkusKafka)
    }
}

/**
 * Gradle Kafka client configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiKafkaExtension @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.javaObjectType)
    val packageName: Property<String> = objects.property(String::class.java)
    val modelPackageName: Property<String> = objects.property(String::class.java)
    val headers: AsyncApiKafkaHeadersExtension =
        objects.newInstance(AsyncApiKafkaHeadersExtension::class.java)
    val springKafka: AsyncApiKafkaSpringKafkaExtension =
        objects.newInstance(AsyncApiKafkaSpringKafkaExtension::class.java)

    fun headers(action: Action<AsyncApiKafkaHeadersExtension>) {
        action.execute(headers)
    }

    fun springKafka(action: Action<AsyncApiKafkaSpringKafkaExtension>) {
        action.execute(springKafka)
    }
}

/**
 * Gradle Kafka header generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiKafkaHeadersExtension @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.javaObjectType)
}

/**
 * Gradle Spring Kafka client generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiKafkaSpringKafkaExtension @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.javaObjectType)
    val producer: AsyncApiKafkaProducerExtension =
        objects.newInstance(AsyncApiKafkaProducerExtension::class.java)
    val consumer: AsyncApiKafkaConsumerExtension =
        objects.newInstance(AsyncApiKafkaConsumerExtension::class.java)

    fun producer(action: Action<AsyncApiKafkaProducerExtension>) {
        action.execute(producer)
    }

    fun consumer(action: Action<AsyncApiKafkaConsumerExtension>) {
        action.execute(consumer)
    }
}

/**
 * Gradle Spring Kafka producer generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiKafkaProducerExtension @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.javaObjectType)
}

/**
 * Gradle Spring Kafka consumer generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiKafkaConsumerExtension @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.javaObjectType)
}

/**
 * Gradle Quarkus Kafka client configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
 */
abstract class AsyncApiQuarkusKafkaExtension @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.javaObjectType)
    val packageName: Property<String> = objects.property(String::class.java)
    val modelPackageName: Property<String> = objects.property(String::class.java)
}
