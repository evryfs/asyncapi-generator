package dev.banking.asyncapi.generator.core.generator.java

import com.github.mustachejava.DefaultMustacheFactory
import dev.banking.asyncapi.generator.core.generator.java.mapper.ImportMapper
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.java.model.JavaRecordTemplate
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import java.io.StringWriter

/**
 * Renders Java class model items as Java record source artifacts.
 *
 * Expected behavior is covered by:
 * - `JavaModelArtifactGeneratorTest`
 * - `JavaModelApprovalTest`
 */
class JavaRecordGenerator(
    val packageName: String,
) {
    private val mustacheFactory = DefaultMustacheFactory("java")
    private val importMapper = ImportMapper(packageName)

    fun render(model: GeneratorItem.ClassModel): GeneratedArtifact {
        val template = mustacheFactory.compile("javaRecord.mustache")

        val fields = model.properties.mapIndexed { index, prop ->
            mapOf(
                "name" to prop.name,
                "type" to prop.typeName,
                "docFirstLine" to prop.docFirstLine,
                "docTailLines" to prop.docTailLines,
                "last" to (index == model.properties.size - 1),
                "componentSeparator" to if (index == model.properties.size - 1) "" else ",",
                "componentSpacer" to if (index == model.properties.size - 1) "" else "\n",
                "annotations" to prop.annotations,
            )
        }

        val imports =
            (
                importMapper.computeImports(model.name, model.properties, includeObjects = false) +
                    model.classAnnotationImports
            )
                .distinct()
                .sorted()
        val implementsClause =
            if (model.implementsInterfaces.isNotEmpty()) {
                " implements " + model.implementsInterfaces.joinToString(", ")
            } else {
                ""
            }

        val data =
            JavaRecordTemplate(
                packageName = model.packageName,
                className = model.name,
                classDocLines = model.description,
                fields = fields,
                imports = imports,
                implementsClause = implementsClause,
                classAnnotations = model.classAnnotations,
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
