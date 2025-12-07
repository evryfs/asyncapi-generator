package dev.banking.asyncapi.generator.core.generator.java

import com.github.mustachejava.DefaultMustacheFactory
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.util.FileUtil
import java.io.File
import java.io.StringWriter

class JavaInterfaceGenerator(
    val outputDir: File,
) {
    private val mustacheFactory = DefaultMustacheFactory("java")

    fun generate(model: GeneratorItem.InterfaceModel) {
        val template = mustacheFactory.compile("javaInterface.mustache")

        val data = object {
            val packageName = model.packageName
            val interfaceName = model.name
            val description = model.description
            val discriminator = model.discriminator
            val hasSubTypes = model.subTypes.isNotEmpty()
            val subTypes = model.subTypes.mapIndexed { index, subType ->
                object {
                    val type = subType.type
                    val name = subType.name
                    val last = index == model.subTypes.size - 1
                }
            }
        }

        val writer = StringWriter()
        template.execute(writer, data).flush()

        val packageDir = FileUtil.packageDirectory(outputDir, model.packageName)
        val outputFile = File(packageDir, "${model.name}.java")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(writer.toString())
    }
}
