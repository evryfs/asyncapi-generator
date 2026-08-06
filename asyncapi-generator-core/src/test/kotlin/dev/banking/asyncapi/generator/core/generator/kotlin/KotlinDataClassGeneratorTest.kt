package dev.banking.asyncapi.generator.core.generator.kotlin

import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.kotlin.model.PropertyModel
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinDataClassGeneratorTest {
    @Test
    fun `render returns source artifact with package-relative path and content`() {
        val generator = KotlinDataClassGenerator("com.example.model")

        val artifact = generator.render(userModel())

        assertEquals(GeneratedArtifactKind.SOURCE, artifact.kind)
        assertEquals("com/example/model/User.kt", artifact.relativePath)
        assertTrue(artifact.content.contains("package com.example.model"))
        assertTrue(artifact.content.contains("data class User"))
        assertTrue(artifact.content.contains("val id: String"))
    }

    private fun userModel(): GeneratorItem.DataClassModel =
        GeneratorItem.DataClassModel(
            name = "User",
            packageName = "com.example.model",
            description = listOf("Generated user model."),
            properties =
                listOf(
                    PropertyModel(
                        name = "id",
                        description = listOf("User identifier."),
                        typeName = "String",
                        defaultValue = null,
                        annotations = emptyList(),
                    ),
                ),
            parentInterfaces = emptyList(),
        )
}
