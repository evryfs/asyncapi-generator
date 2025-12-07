package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin

import com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import java.io.File

class KotlinGenerator(
    private val packageName: String,
    private val outputDir: File,
    private val generationModel: List<GeneratorItem>
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

    fun generate() {
        generationModel.forEach { item ->
            when (item) {
                is GeneratorItem.DataClassModel -> dataClassGenerator.generate(item)
                is GeneratorItem.EnumClassModel -> enumGenerator.generate(item)
                is GeneratorItem.SealedInterfaceModel -> sealedInterfaceGenerator.generate(item)
                else -> { /* Ignore other types if mixed */ }
            }
        }
    }
}
