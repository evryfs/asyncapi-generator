package dev.banking.asyncapi.generator.core.generator.kotlin

import com.github.mustachejava.DefaultMustacheFactory
import dev.banking.asyncapi.generator.core.generator.kotlin.mapper.ImportMapper
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.kotlin.model.KotlinClassTemplate
import dev.banking.asyncapi.generator.core.generator.util.FileUtil
import java.io.File
import java.io.StringWriter

class KotlinDataClassGenerator(
    val outputDir: File,
    val packageName: String,
) {
    private val mustacheFactory = DefaultMustacheFactory("kotlin")

    private val importMapper = ImportMapper(packageName)

    fun generate(model: GeneratorItem.DataClassModel) {

        val template = mustacheFactory.compile("dataClass.mustache")

        val fields = model.properties.mapIndexed { index, prop ->
               mapOf(
                   "name" to prop.name,
                   "type" to prop.baseType,
                   "docFirstLine" to prop.docFirstLine,
                   "docTailLines" to prop.docTailLines,
                   "nullable" to prop.isNullable,
                   "defaultValue" to prop.defaultValue,
                   "last" to (index == model.properties.size - 1),
                   "annotations" to prop.annotations
               )
           }

        val imports = importMapper.computeImports(model.name, model.properties)
        val implementsClause = if (model.parentInterfaces.isNotEmpty()) {
            " : " + model.parentInterfaces.joinToString(", ")
        } else {
            ""
        }

        val data = KotlinClassTemplate(
            packageName = model.packageName,
            className = model.name,
            classDocLines = model.description,
            fields = fields,
            imports = imports,
            implementsClause = implementsClause,
            classAnnotations = model.classAnnotations
        )

        val writer = StringWriter()
        template.execute(writer, data).flush()

        val packageDir = FileUtil.packageDirectory(outputDir, model.packageName)
        val outputFile = File(packageDir, "${model.name}.kt")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(writer.toString())
    }
}
