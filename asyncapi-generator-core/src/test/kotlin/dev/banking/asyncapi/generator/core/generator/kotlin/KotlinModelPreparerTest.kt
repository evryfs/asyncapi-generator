package dev.banking.asyncapi.generator.core.generator.kotlin

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class KotlinModelPreparerTest {
    private val preparer = KotlinModelPreparer()
    private val fixtures = GenerationInputFixtures()

    @Test
    fun `prepare maps generation input into Kotlin model items`() {
        val items =
            preparer.prepare(
                input = fixtures.generationInputWithObjectEnumAndPrimitive(),
                packageName = "com.example.model",
                annotation =
                    QualifiedTypeName.fromConfigurationValue(
                        value = "jakarta.validation.Valid",
                        path = "modelConfig.modelAnnotation",
                    ),
            )

        assertEquals(listOf("User", "Status"), items.map { it.name })

        val user = items.filterIsInstance<GeneratorItem.DataClassModel>().single()
        assertEquals("com.example.model", user.packageName)
        assertEquals(listOf("Command"), user.parentInterfaces)
        assertEquals(listOf("@Valid"), user.classAnnotations)
        assertEquals(listOf("jakarta.validation.Valid"), user.classAnnotationImports)
        assertEquals("id", user.properties.single().name)
        assertEquals("String", user.properties.single().typeName)

        val status = items.filterIsInstance<GeneratorItem.EnumClassModel>().single()
        assertEquals("com.example.model", status.packageName)
        assertEquals(listOf("ACTIVE", "INACTIVE"), status.values)
    }
}
