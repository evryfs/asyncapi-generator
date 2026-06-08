package dev.banking.asyncapi.generator.core.generator.kotlin

import com.github.mustachejava.DefaultMustacheFactory
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.output.FileSystemGeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import java.io.File
import java.io.StringWriter

/**
 * Renders Kotlin sealed interface model items into source artifacts.
 *
 * Expected behavior is covered by:
 * - `KotlinModelArtifactGeneratorTest`
 * - `KotlinModelApprovalTest`
 */
class KotlinSealedInterfaceGenerator(
    val outputDir: File,
) {
    private val mustacheFactory = DefaultMustacheFactory("kotlin")

    fun generate(model: GeneratorItem.SealedInterfaceModel) {
        val artifact = render(model)
        FileSystemGeneratedArtifactWriter(outputDir, outputDir)
            .write(GenerationResult.of(artifact))
    }

    fun render(model: GeneratorItem.SealedInterfaceModel): GeneratedArtifact {
        val template = mustacheFactory.compile("sealedInterface.mustache")

        val templateData = object {
            val packageName = model.packageName
            val interfaceName = model.name
            val description = model.description
        }

        val writer = StringWriter()
        template.execute(writer, templateData).flush()

        return GeneratedArtifact(
            relativePath = GeneratedArtifactPaths.fromNamespace(model.packageName, "${model.name}.kt"),
            content = writer.toString(),
            kind = GeneratedArtifactKind.SOURCE,
        )
    }
}
