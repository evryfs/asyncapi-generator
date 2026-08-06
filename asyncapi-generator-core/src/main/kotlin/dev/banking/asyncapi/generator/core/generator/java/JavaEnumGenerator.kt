package dev.banking.asyncapi.generator.core.generator.java

import com.github.mustachejava.DefaultMustacheFactory
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import java.io.StringWriter

/**
 * Renders Java enum model items into source artifacts.
 *
 * Expected behavior is covered by:
 * - `JavaModelArtifactGeneratorTest`
 * - `JavaModelApprovalTest`
 */
class JavaEnumGenerator {
    private val mustacheFactory = DefaultMustacheFactory("java")

    fun render(model: GeneratorItem.EnumModel): GeneratedArtifact {
        val template = mustacheFactory.compile("javaEnum.mustache")

        val writer = StringWriter()
        template.execute(writer, model).flush()

        return GeneratedArtifact(
            relativePath = GeneratedArtifactPaths.fromNamespace(model.packageName, "${model.name}.java"),
            content = writer.toString(),
            kind = GeneratedArtifactKind.SOURCE,
        )
    }
}
