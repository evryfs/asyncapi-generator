package dev.banking.asyncapi.generator.core.generator.java.kafka.spring

import com.github.mustachejava.DefaultMustacheFactory
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.util.FileUtil
import java.io.File
import java.io.StringWriter

class JavaSpringKafkaSimpleProducerGenerator(
    private val outputDir: File,
) {
    private val mustacheFactory = DefaultMustacheFactory("java")

    fun generate(model: GeneratorItem.KafkaProducerClass) {
        val template = mustacheFactory.compile("spring-kafka-simple-producer.mustache")
        val writer = StringWriter()
        template.execute(writer, model).flush()

        val packageDir = FileUtil.packageDirectory(outputDir, model.packageName)
        val outputFile = File(packageDir, "${model.name}.java")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(writer.toString())
    }
}
