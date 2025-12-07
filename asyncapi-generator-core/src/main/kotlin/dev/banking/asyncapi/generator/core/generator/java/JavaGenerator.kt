package dev.banking.asyncapi.generator.core.generator.java

import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import java.io.File

class JavaGenerator(
    private val packageName: String,
    private val outputDir: File,
    private val generationModel: List<GeneratorItem>
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
        generationModel.forEach { item ->
            when (item) {
                is GeneratorItem.ClassModel -> classGenerator.generate(item)
                is GeneratorItem.EnumModel -> enumGenerator.generate(item)
                is GeneratorItem.InterfaceModel -> interfaceGenerator.generate(item)
                else -> { /* Ignore other types if mixed */ }
            }
        }
    }
}
