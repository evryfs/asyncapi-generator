package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin

import com.github.mustachejava.DefaultMustacheFactory
import com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.mapper.ImportMapper
import com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.model.KotlinClassTemplate
import com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.model.KotlinFieldTemplate
import com.tietoevry.banking.asyncapi.generator.core.generator.util.FileUtil
import java.io.File
import java.io.StringWriter

class KotlinDataClassGenerator(
    val outputDir: File,
    val packageName: String,
) {
    private val mustacheFactory = DefaultMustacheFactory("kotlin")

    // The ImportMapper is still needed to calculate imports from the final type names.
    private val importMapper = ImportMapper(packageName)

    fun generate(model: GeneratorItem.DataClassModel) {

        val template = mustacheFactory.compile("dataClass.mustache")

        // Temporarily adapt our new rich PropertyModel to the old KotlinFieldTemplate
        // to avoid changing the mustache template for now.
        val fields = model.properties.mapIndexed { index, prop ->
            KotlinFieldTemplate(
                name = prop.name,
                type = prop.typeName.removeSuffix("?"),
                docFirstLine = prop.description.firstOrNull(),
                docTailLines = prop.description.drop(1),
                nullable = prop.typeName.endsWith("?"),
                defaultValue = prop.defaultValue,
                last = index == model.properties.size - 1,
                annotations = prop.annotations
            )
        }

        val imports = importMapper.computeImports(model.name, fields)
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
            implementsClause = implementsClause
        )

        val writer = StringWriter()
        template.execute(writer, data).flush()

        val packageDir = FileUtil.packageDirectory(outputDir, model.packageName)
        val outputFile = File(packageDir, "${model.name}.kt")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(writer.toString())
    }
}
