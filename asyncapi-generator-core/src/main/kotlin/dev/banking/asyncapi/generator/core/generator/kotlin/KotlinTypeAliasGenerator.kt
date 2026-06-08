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
 * Renders Kotlin type alias model items into source artifacts.
 *
 * Expected behavior is covered by:
 * - `KotlinModelArtifactGeneratorTest`
 * - `KotlinModelApprovalTest`
 */
class KotlinTypeAliasGenerator(
    private val outputDir: File,
) {
    private val mustacheFactory = DefaultMustacheFactory("kotlin")

    fun generate(model: GeneratorItem.TypeAliasModel) {
        val artifact = render(model)
        FileSystemGeneratedArtifactWriter(outputDir, outputDir)
            .write(GenerationResult.of(artifact))
    }

    fun render(model: GeneratorItem.TypeAliasModel): GeneratedArtifact {
        val template = mustacheFactory.compile("typeAlias.mustache")

        val data =
            mapOf(
                "packageName" to model.packageName,
                "name" to model.name,
                "aliasType" to model.aliasType,
                "imports" to model.imports,
            )

        val writer = StringWriter()
        template.execute(writer, data).flush()

        return GeneratedArtifact(
            relativePath = GeneratedArtifactPaths.fromNamespace(model.packageName, "${model.name}.kt"),
            content = writer.toString(),
            kind = GeneratedArtifactKind.SOURCE,
        )
    }
}
