package com.tietoevry.banking.asyncapi.generator.core.generator.java.kafka.spring

import com.github.mustachejava.DefaultMustacheFactory
import com.tietoevry.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import com.tietoevry.banking.asyncapi.generator.core.generator.util.FileUtil
import java.io.File
import java.io.StringWriter

class JavaSpringKafkaListenerGenerator(
    private val outputDir: File,
) {
    private val mustacheFactory = DefaultMustacheFactory("java")

    fun generate(model: GeneratorItem.KafkaListenerClass) {
        val template = mustacheFactory.compile("spring-kafka-listener.mustache")
        val writer = StringWriter()
        template.execute(writer, model).flush()

        val packageDir = FileUtil.packageDirectory(outputDir, model.packageName)
        val file = File(packageDir, "${model.name}.java")
        file.writeText(writer.toString())
    }
}
