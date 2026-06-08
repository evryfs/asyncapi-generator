package dev.banking.asyncapi.generator.core.generator.java

import com.github.mustachejava.DefaultMustacheFactory
import dev.banking.asyncapi.generator.core.generator.java.mapper.ImportMapper
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.java.model.JavaClassTemplate
import dev.banking.asyncapi.generator.core.generator.output.FileSystemGeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import java.io.File
import java.io.StringWriter

/**
 * Renders Java class model items into source artifacts.
 *
 * Expected behavior is covered by:
 * - `JavaModelArtifactGeneratorTest`
 * - `JavaModelApprovalTest`
 */
class JavaClassGenerator(
    val outputDir: File,
    val packageName: String,
) {
    private val mustacheFactory = DefaultMustacheFactory("java")
    private val importMapper = ImportMapper(packageName)

    fun generate(model: GeneratorItem.ClassModel) {
        val artifact = render(model)
        FileSystemGeneratedArtifactWriter(outputDir, outputDir)
            .write(GenerationResult.of(artifact))
    }

    fun render(model: GeneratorItem.ClassModel): GeneratedArtifact {
        val template = mustacheFactory.compile("javaClass.mustache")

        val fields = model.properties.mapIndexed { index, prop ->
            mapOf(
                "name" to prop.name,
                "type" to prop.typeName,
                "getterName" to prop.getterName,
                "setterName" to prop.setterName,
                "docFirstLine" to prop.docFirstLine,
                "docTailLines" to prop.docTailLines,
                "last" to (index == model.properties.size - 1),
                "annotations" to prop.annotations
            )
        }

        val imports = importMapper.computeImports(model.name, model.properties)
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

        return GeneratedArtifact(
            relativePath = GeneratedArtifactPaths.fromNamespace(model.packageName, "${model.name}.java"),
            content = writer.toString(),
            kind = GeneratedArtifactKind.SOURCE,
        )
    }
}
