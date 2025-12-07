package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.kafka.spring

import com.github.mustachejava.DefaultMustacheFactory
import com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import com.tietoevry.banking.asyncapi.generator.core.generator.util.FileUtil
import java.io.File
import java.io.StringWriter

class KotlinSpringKafkaProducerGenerator(
    private val outputDir: File,
) {
    private val mustacheFactory = DefaultMustacheFactory("kotlin")

    fun generate(model: GeneratorItem.KafkaProducerClass) {
        val template = mustacheFactory.compile("spring-kafka-producer.mustache")
        val writer = StringWriter()
        template.execute(writer, model).flush()

        val packageDir = FileUtil.packageDirectory(outputDir, model.packageName)
        val outputFile = File(packageDir, "${model.name}.kt")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(writer.toString())
    }
}
