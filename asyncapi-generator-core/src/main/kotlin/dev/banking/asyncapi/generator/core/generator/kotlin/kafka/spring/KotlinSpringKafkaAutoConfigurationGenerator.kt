package dev.banking.asyncapi.generator.core.generator.kotlin.kafka.spring

import com.github.mustachejava.DefaultMustacheFactory
import dev.banking.asyncapi.generator.core.generator.kafka.spring.AutoConfigurationModel
import dev.banking.asyncapi.generator.core.generator.util.FileUtil
import java.io.File
import java.io.StringWriter

class KotlinSpringKafkaAutoConfigurationGenerator(
    private val outputDir: File,
) {
    private val mustacheFactory = DefaultMustacheFactory("kotlin")

    fun generate(model: AutoConfigurationModel) {
        val template = mustacheFactory.compile("spring-kafka-autoconfiguration.mustache")
        val writer = StringWriter()
        template.execute(writer, model).flush()

        val packageDir = FileUtil.packageDirectory(outputDir, model.packageName)
        val outputFile = File(packageDir, "${model.className}.kt")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(writer.toString())
    }
}
