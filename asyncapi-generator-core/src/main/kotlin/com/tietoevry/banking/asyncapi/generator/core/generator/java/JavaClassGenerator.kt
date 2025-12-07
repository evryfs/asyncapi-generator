package com.tietoevry.banking.asyncapi.generator.core.generator.java

import com.github.mustachejava.DefaultMustacheFactory
import com.tietoevry.banking.asyncapi.generator.core.generator.java.mapper.ImportMapper
import com.tietoevry.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import com.tietoevry.banking.asyncapi.generator.core.generator.java.model.JavaClassTemplate
import com.tietoevry.banking.asyncapi.generator.core.generator.java.model.JavaFieldTemplate
import com.tietoevry.banking.asyncapi.generator.core.generator.util.FileUtil
import java.io.File
import java.io.StringWriter

class JavaClassGenerator(
    val outputDir: File,
    val packageName: String,
) {
    private val mustacheFactory = DefaultMustacheFactory("java")
    private val importMapper = ImportMapper(packageName)

    fun generate(model: GeneratorItem.ClassModel) {
        val template = mustacheFactory.compile("javaClass.mustache")

        val fields = model.properties.mapIndexed { index, prop ->
            JavaFieldTemplate(
                name = prop.name,
                type = prop.typeName,
                getterName = prop.getterName,
                setterName = prop.setterName,
                docFirstLine = prop.description.firstOrNull(),
                docTailLines = prop.description.drop(1),
                last = index == model.properties.size - 1,
                annotations = prop.annotations
            )
        }

        val imports = importMapper.computeImports(model.name, fields)
        val implementsClause = if (model.implementsInterfaces.isNotEmpty()) {
            " implements " + model.implementsInterfaces.joinToString(", ")
        } else {
            ""
        }

        val data = JavaClassTemplate(
            packageName = model.packageName,
            className = model.name,
            classDocLines = model.description,
            fields = fields,
            allFields = fields,
            imports = imports,
            implementsClause = implementsClause
        )

        val writer = StringWriter()
        template.execute(writer, data).flush()

        val packageDir = FileUtil.packageDirectory(outputDir, model.packageName)
        val outputFile = File(packageDir, "${model.name}.java")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(writer.toString())
    }
}
