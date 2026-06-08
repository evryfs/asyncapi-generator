package dev.banking.asyncapi.generator.core.generator.java

import com.github.mustachejava.DefaultMustacheFactory
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.output.FileSystemGeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import java.io.File
import java.io.StringWriter

/**
 * Renders Java interface model items into source artifacts.
 *
 * Expected behavior is covered by:
 * - `JavaModelArtifactGeneratorTest`
 * - `JavaModelApprovalTest`
 */
class JavaInterfaceGenerator(
    val outputDir: File,
) {
    private val mustacheFactory = DefaultMustacheFactory("java")

    fun generate(model: GeneratorItem.InterfaceModel) {
        val artifact = render(model)
        FileSystemGeneratedArtifactWriter(outputDir, outputDir)
            .write(GenerationResult.of(artifact))
    }

    fun render(model: GeneratorItem.InterfaceModel): GeneratedArtifact {
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

        return GeneratedArtifact(
            relativePath = GeneratedArtifactPaths.fromNamespace(model.packageName, "${model.name}.java"),
            content = writer.toString(),
            kind = GeneratedArtifactKind.SOURCE,
        )
    }
}
