package dev.banking.asyncapi.generator.core.generator.java

import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.output.FileSystemGeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import java.io.File

/**
 * Renders Java model items into a generation result before writing them.
 *
 * Expected behavior is covered by:
 * - `JavaGeneratorTest`
 * - `JavaModelApprovalTest`
 */
class JavaGenerator(
    private val packageName: String,
    private val outputDir: File,
    private val generationModel: List<GeneratorItem>,
) {
    private val classGenerator: JavaClassGenerator by lazy {
        JavaClassGenerator(outputDir, packageName)
    }
    private val enumGenerator: JavaEnumGenerator by lazy {
        JavaEnumGenerator(outputDir)
    }
    private val interfaceGenerator: JavaInterfaceGenerator by lazy {
        JavaInterfaceGenerator(outputDir)
    }

    fun generate() {
        FileSystemGeneratedArtifactWriter(outputDir, outputDir)
            .write(render())
    }

    fun render(): GenerationResult =
        GenerationResult(
            generationModel.mapNotNull { item ->
                when (item) {
                    is GeneratorItem.ClassModel -> classGenerator.render(item)
                    is GeneratorItem.EnumModel -> enumGenerator.render(item)
                    is GeneratorItem.InterfaceModel -> interfaceGenerator.render(item)
                    else -> null
                }
            },
        )
}
