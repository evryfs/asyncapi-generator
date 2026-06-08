package dev.banking.asyncapi.generator.core.generator.kotlin

import dev.banking.asyncapi.generator.core.generator.output.FileSystemGeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import java.io.File

/**
 * Renders Kotlin model items into a generation result before writing them.
 *
 * Expected behavior is covered by:
 * - `KotlinGeneratorTest`
 * - `KotlinModelApprovalTest`
 */
class KotlinGenerator(
    private val packageName: String,
    private val outputDir: File,
    private val generationModel: List<GeneratorItem>,
) {
    private val dataClassGenerator: KotlinDataClassGenerator by lazy {
        KotlinDataClassGenerator(outputDir, packageName)
    }
    private val sealedInterfaceGenerator: KotlinSealedInterfaceGenerator by lazy {
        KotlinSealedInterfaceGenerator(outputDir)
    }
    private val enumGenerator: KotlinEnumGenerator by lazy {
        KotlinEnumGenerator(outputDir)
    }
    private val typeAliasGenerator: KotlinTypeAliasGenerator by lazy {
        KotlinTypeAliasGenerator(outputDir)
    }

    fun generate() {
        FileSystemGeneratedArtifactWriter(outputDir, outputDir)
            .write(render())
    }

    fun render(): GenerationResult =
        GenerationResult(
            generationModel.mapNotNull { item ->
                when (item) {
                    is GeneratorItem.DataClassModel -> dataClassGenerator.render(item)
                    is GeneratorItem.EnumClassModel -> enumGenerator.render(item)
                    is GeneratorItem.SealedInterfaceModel -> sealedInterfaceGenerator.render(item)
                    is GeneratorItem.TypeAliasModel -> typeAliasGenerator.render(item)
                    else -> null
                }
            },
        )
}
