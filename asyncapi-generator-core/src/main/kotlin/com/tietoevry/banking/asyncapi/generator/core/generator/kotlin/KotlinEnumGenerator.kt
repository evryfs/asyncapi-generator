package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin

import com.github.mustachejava.DefaultMustacheFactory
import com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import com.tietoevry.banking.asyncapi.generator.core.generator.util.FileUtil
import java.io.File
import java.io.StringWriter

class KotlinEnumGenerator(
    val outputDir: File,
) {
    private val mustacheFactory = DefaultMustacheFactory("kotlin")

    fun generate(model: GeneratorItem.EnumClassModel) {
        val template = mustacheFactory.compile("enum.mustache")

        val writer = StringWriter()
        template.execute(writer, model).flush()

        val packageDir = FileUtil.packageDirectory(outputDir, model.packageName)
        val outputFile = File(packageDir, "${model.name}.kt")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(writer.toString())
    }
}

