package dev.banking.asyncapi.generator.core.generator.java

import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult

/**
 * Renders Java model items into a generation result before writing them.
 */
class JavaGenerator(
    private val packageName: String,
    private val generationModel: List<GeneratorItem>,
    private val javaModelType: JavaModelType = JavaModelType.CLASS,
) {
    private val classGenerator: JavaClassGenerator by lazy {
        JavaClassGenerator(packageName)
    }
    private val recordGenerator: JavaRecordGenerator by lazy {
        JavaRecordGenerator(packageName)
    }
    private val enumGenerator: JavaEnumGenerator by lazy {
        JavaEnumGenerator()
    }
    private val interfaceGenerator: JavaInterfaceGenerator by lazy {
        JavaInterfaceGenerator()
    }

    fun render(): GenerationResult =
        GenerationResult(
            generationModel.mapNotNull { item ->
                when (item) {
                    is GeneratorItem.ClassModel ->
                        when (javaModelType) {
                            JavaModelType.CLASS -> classGenerator.render(item)
                            JavaModelType.RECORD -> recordGenerator.render(item)
                        }
                    is GeneratorItem.EnumModel -> enumGenerator.render(item)
                    is GeneratorItem.InterfaceModel -> interfaceGenerator.render(item)
                    else -> null
                }
            },
        )
}
