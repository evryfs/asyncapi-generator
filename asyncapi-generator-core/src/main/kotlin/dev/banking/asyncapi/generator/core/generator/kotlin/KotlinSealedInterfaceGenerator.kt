package dev.banking.asyncapi.generator.core.generator.kotlin

import dev.banking.asyncapi.generator.core.generator.util.SourceMustacheFactory
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import java.io.StringWriter

/**
 * Renders Kotlin sealed interface model items into source artifacts.
 */
class KotlinSealedInterfaceGenerator {
    private val mustacheFactory = SourceMustacheFactory("kotlin")

    fun render(model: GeneratorItem.SealedInterfaceModel): GeneratedArtifact {
        val template = mustacheFactory.compile("sealedInterface.mustache")

        val templateData = object {
            val packageName = model.packageName
            val interfaceName = model.name
            val description = model.description
            val discriminator = model.discriminator
            val hasSubTypes = model.subTypes.isNotEmpty()
            val subTypes = model.subTypes
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
