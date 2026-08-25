package dev.banking.asyncapi.generator.core.generator.kotlin

import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult

/**
 * Renders Kotlin model items into a generation result before writing them.
 */
class KotlinGenerator(
    private val packageName: String,
    private val generationModel: List<GeneratorItem>,
) {
    private val dataClassGenerator: KotlinDataClassGenerator by lazy {
        KotlinDataClassGenerator(packageName)
    }
    private val sealedInterfaceGenerator: KotlinSealedInterfaceGenerator by lazy {
        KotlinSealedInterfaceGenerator()
    }
    private val enumGenerator: KotlinEnumGenerator by lazy {
        KotlinEnumGenerator()
    }
    private val typeAliasGenerator: KotlinTypeAliasGenerator by lazy {
        KotlinTypeAliasGenerator()
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
