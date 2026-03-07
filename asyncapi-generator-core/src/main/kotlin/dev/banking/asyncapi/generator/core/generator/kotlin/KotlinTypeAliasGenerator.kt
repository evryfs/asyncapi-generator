package dev.banking.asyncapi.generator.core.generator.kotlin

import com.github.mustachejava.DefaultMustacheFactory
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.util.FileUtil
import java.io.File
import java.io.StringWriter

class KotlinTypeAliasGenerator(
    private val outputDir: File,
    private val packageName: String,
) {
    private val mustacheFactory = DefaultMustacheFactory("kotlin")

    fun generate(model: GeneratorItem.TypeAliasModel) {
        val template = mustacheFactory.compile("typeAlias.mustache")

        val data =
            mapOf(
                "packageName" to model.packageName,
                "name" to model.name,
                "aliasType" to model.aliasType,
                "imports" to model.imports,
            )

        val writer = StringWriter()
        template.execute(writer, data).flush()

        val packageDir = FileUtil.packageDirectory(outputDir, model.packageName)
        val outputFile = File(packageDir, "${model.name}.kt")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(writer.toString())
    }
}
