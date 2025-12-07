package com.tietoevry.banking.asyncapi.generator.core.generator.java

import com.github.mustachejava.DefaultMustacheFactory
import com.tietoevry.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import com.tietoevry.banking.asyncapi.generator.core.generator.util.FileUtil
import java.io.File
import java.io.StringWriter

class JavaEnumGenerator(
    val outputDir: File,
) {
    private val mustacheFactory = DefaultMustacheFactory("java")

    fun generate(model: GeneratorItem.EnumModel) {
        val template = mustacheFactory.compile("javaEnum.mustache")

        val writer = StringWriter()
        template.execute(writer, model).flush()

        val packageDir = FileUtil.packageDirectory(outputDir, model.packageName)
        val outputFile = File(packageDir, "${model.name}.java")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(writer.toString())
    }
}
