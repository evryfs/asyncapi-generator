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

    fun Mojo.clientConfig(value: MavenClientConfiguration?) {
        writeField("clientConfig", value)
    }

    fun Mojo.generatorName(value: String?) {
        writeField("generatorName", value)
    }

    private fun Mojo.writeField(name: String, value: Any?) {
        val field = this.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(this, value)
    }

    fun modelConfig(
        modelAnnotation: String? = null,
        modelType: String? = null,
    ): MavenModelConfiguration =
        MavenModelConfiguration().apply {
            this.modelAnnotation = modelAnnotation
            this.modelType = modelType
        }

    fun clientConfig(
        clientType: String? = null,
        clientContract: String? = null,
        producer: MavenProducerConfiguration? = null,
        consumer: MavenConsumerConfiguration? = null,
        topicParameterProperties: Map<String, String>? = null,
        validationAnnotations: MavenValidationAnnotationsConfiguration? = null,
    ): MavenClientConfiguration =
        MavenClientConfiguration().apply {
            this.clientType = clientType
            this.clientContract = clientContract
            this.producer = producer
            this.consumer = consumer
            this.topicParameterProperties = topicParameterProperties
            this.validationAnnotations = validationAnnotations
        }

    fun producer(
        enabled: Boolean? = null,
        additionalPayloadTypes: List<String>? = null,
    ): MavenProducerConfiguration =
        MavenProducerConfiguration().apply {
            this.enabled = enabled
            this.additionalPayloadTypes = additionalPayloadTypes
        }

    fun consumer(enabled: Boolean? = null): MavenConsumerConfiguration =
        MavenConsumerConfiguration().apply {
            this.enabled = enabled
        }

    fun validationAnnotations(
        clientContract: String? = null,
        payloadParameter: String? = null,
    ): MavenValidationAnnotationsConfiguration =
        MavenValidationAnnotationsConfiguration().apply {
            this.clientContract = clientContract
            this.payloadParameter = payloadParameter
        }
}
