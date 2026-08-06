package dev.banking.asyncapi.generator.core.generator.kotlin

import com.github.mustachejava.DefaultMustacheFactory
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import java.io.StringWriter

/**
 * Renders Kotlin enum model items into source artifacts.
 *
 * Expected behavior is covered by:
 * - `KotlinModelArtifactGeneratorTest`
 * - `KotlinModelApprovalTest`
 */
class KotlinEnumGenerator {
    private val mustacheFactory = DefaultMustacheFactory("kotlin")

    fun render(model: GeneratorItem.EnumClassModel): GeneratedArtifact {
        val template = mustacheFactory.compile("enum.mustache")

        val writer = StringWriter()
        template.execute(writer, model).flush()

        return GeneratedArtifact(
            relativePath = GeneratedArtifactPaths.fromNamespace(model.packageName, "${model.name}.kt"),
            content = writer.toString(),
            kind = GeneratedArtifactKind.SOURCE,
        )
    }
}
