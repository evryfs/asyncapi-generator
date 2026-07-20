package dev.banking.asyncapi.generator.maven.plugin

import org.apache.maven.plugin.Mojo
import java.io.File

object MavenTestHelper {

    fun outputPath(path: String): File =
        File(path).apply { mkdirs() }

    fun inputPath(path: String): File =
        File("src/test/resources/$path").also {
            require(it.exists()) { "Missing document test file: ${it.absolutePath}" }
        }

    fun Mojo.project(value: Any) {
        writeField("project", value)
    }

    fun Mojo.inputSpec(value: Any) {
        writeField("inputSpec", value)
    }

    fun Mojo.outputFile(value: Any?) {
        writeField("outputFile", value)
    }

    fun Mojo.outputDirectory(value: Any) {
        writeField("outputDirectory", value)
    }

    fun Mojo.modelPackage(value: String?) {
        writeField("modelPackage", value)
    }

    fun Mojo.clientPackage(value: String?) {
        writeField("clientPackage", value)
    }

    fun Mojo.schemaPackage(value: String?) {
        writeField("schemaPackage", value)
    }

    fun Mojo.modelConfig(value: MavenModelConfiguration?) {
        writeField("modelConfig", value)
    }

    fun Mojo.schemaConfig(value: MavenSchemaConfiguration?) {
        writeField("schemaConfig", value)
    }

    fun Mojo.clientConfig(value: MavenClientConfiguration?) {
        writeField("clientConfig", value)
    }

    fun Mojo.generatorName(value: String) {
        writeField("generatorName", value)
    }

    private fun Mojo.writeField(name: String, value: Any?) {
        val field = this.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(this, value)
    }

    fun modelConfig(
        modelAnnotation: String? = null,
        javaModelType: String? = null,
        protobufModelType: String? = null,
    ): MavenModelConfiguration =
        MavenModelConfiguration().apply {
            this.modelAnnotation = modelAnnotation
            this.javaModelType = javaModelType
            this.protobufModelType = protobufModelType
        }

    fun schemaConfig(
        avroProjection: MavenAvroProjectionConfiguration? = null,
        nativeAvro: MavenNativeAvroConfiguration? = null,
        nativeProtobuf: MavenNativeProtobufConfiguration? = null,
    ): MavenSchemaConfiguration =
        MavenSchemaConfiguration().apply {
            this.avroProjection = avroProjection
            this.nativeAvro = nativeAvro
            this.nativeProtobuf = nativeProtobuf
        }

    fun avroProjection(enabled: Boolean? = null): MavenAvroProjectionConfiguration =
        MavenAvroProjectionConfiguration().apply {
            this.enabled = enabled
        }

    fun nativeAvro(
        enabled: Boolean? = null,
        generateSpecificRecords: Boolean? = null,
    ): MavenNativeAvroConfiguration =
        MavenNativeAvroConfiguration().apply {
            this.enabled = enabled
            this.generateSpecificRecords = generateSpecificRecords
        }

    fun nativeProtobuf(enabled: Boolean? = null): MavenNativeProtobufConfiguration =
        MavenNativeProtobufConfiguration().apply {
            this.enabled = enabled
        }

    fun clientConfig(
        clientType: String? = null,
        clientContract: String? = null,
        generateProducer: Boolean? = null,
        generateConsumer: Boolean? = null,
        producerRecordValueType: String? = null,
    ): MavenClientConfiguration =
        MavenClientConfiguration().apply {
            this.clientType = clientType
            this.clientContract = clientContract
            this.generateProducer = generateProducer
            this.generateConsumer = generateConsumer
            this.producerRecordValueType = producerRecordValueType
        }
}
