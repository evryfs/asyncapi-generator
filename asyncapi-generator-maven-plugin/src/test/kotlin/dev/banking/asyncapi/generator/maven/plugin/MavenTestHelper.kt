package dev.banking.asyncapi.generator.maven.plugin

import org.apache.maven.plugin.Mojo
import java.io.File

object MavenTestHelper {

    fun outputPath(path: String): File =
        File(path).apply { mkdirs() }

    fun inputPath(path: String): File =
        File("src/test/resources/$path").also {
            require(it.exists()) { "Missing YAML test file: ${it.absolutePath}" }
        }

    fun Mojo.project(value: Any) {
        writeField("project", value)
    }

    fun Mojo.inputFile(value: Any) {
        writeField("inputFile", value)
    }

    fun Mojo.outputFile(value: Any?) {
        writeField("outputFile", value)
    }

    fun Mojo.codegenOutputDirectory(value: Any) {
        writeField("codegenOutputDirectory", value)
    }

    fun Mojo.resourceOutputDirectory(value: Any) {
        writeField("resourceOutputDirectory", value)
    }

    fun Mojo.javaSourceOutputDirectory(value: Any) {
        writeField("javaSourceOutputDirectory", value)
    }

    fun Mojo.models(value: MavenModelGenerationConfiguration?) {
        writeField("models", value)
    }

    fun Mojo.schemas(value: MavenSchemaGenerationConfiguration?) {
        writeField("schemas", value)
    }

    fun Mojo.clients(value: MavenClientGenerationConfiguration?) {
        writeField("clients", value)
    }

    fun Mojo.generatorName(value: String) {
        writeField("generatorName", value)
    }

    private fun Mojo.writeField(name: String, value: Any?) {
        val field = this.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(this, value)
    }

    fun models(
        packageName: String? = null,
        annotation: String? = null,
        javaModelType: String? = null,
        enabled: Boolean? = null,
    ): MavenModelGenerationConfiguration =
        MavenModelGenerationConfiguration().apply {
            this.packageName = packageName
            this.annotation = annotation
            this.javaModelType = javaModelType
            this.enabled = enabled
        }

    fun schemas(
        avroProjection: MavenAvroProjectionConfiguration? = null,
        nativeAvro: MavenNativeAvroConfiguration? = null,
        nativeProtobuf: MavenNativeProtobufConfiguration? = null,
    ): MavenSchemaGenerationConfiguration =
        MavenSchemaGenerationConfiguration().apply {
            this.avroProjection = avroProjection
            this.nativeAvro = nativeAvro
            this.nativeProtobuf = nativeProtobuf
        }

    fun avroProjection(
        packageName: String? = null,
        enabled: Boolean? = null,
    ): MavenAvroProjectionConfiguration =
        MavenAvroProjectionConfiguration().apply {
            this.packageName = packageName
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

    fun nativeProtobuf(
        enabled: Boolean? = null,
        generateJavaMessageTypes: Boolean? = null,
    ): MavenNativeProtobufConfiguration =
        MavenNativeProtobufConfiguration().apply {
            this.enabled = enabled
            this.generateJavaMessageTypes = generateJavaMessageTypes
        }

    fun clients(
        springKafka: MavenSpringKafkaConfiguration? = null,
        quarkusKafka: MavenQuarkusKafkaConfiguration? = null,
    ): MavenClientGenerationConfiguration =
        MavenClientGenerationConfiguration().apply {
            this.springKafka = springKafka
            this.quarkusKafka = quarkusKafka
        }

    fun springKafka(
        packageName: String? = null,
        modelPackageName: String? = null,
        enabled: Boolean? = null,
    ): MavenSpringKafkaConfiguration =
        MavenSpringKafkaConfiguration().apply {
            this.packageName = packageName
            this.modelPackageName = modelPackageName
            this.enabled = enabled
        }

    fun quarkusKafka(
        packageName: String? = null,
        modelPackageName: String? = null,
        enabled: Boolean? = null,
    ): MavenQuarkusKafkaConfiguration =
        MavenQuarkusKafkaConfiguration().apply {
            this.packageName = packageName
            this.modelPackageName = modelPackageName
            this.enabled = enabled
        }
}
