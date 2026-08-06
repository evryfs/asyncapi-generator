package dev.banking.asyncapi.generator.core.generator.kotlin

import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinModelArtifactGeneratorTest {
    @Test
    fun `enum render returns source artifact with package-relative path and content`() {
        val generator = KotlinEnumGenerator()
        val enumModel =
            GeneratorItem.EnumClassModel(
                name = "Status",
                packageName = "com.example.model",
                description = emptyList(),
                values = listOf("ACTIVE", "INACTIVE"),
            )

        val artifact = generator.render(enumModel)

        assertEquals(GeneratedArtifactKind.SOURCE, artifact.kind)
        assertEquals("com/example/model/Status.kt", artifact.relativePath)
        assertTrue(artifact.content.contains("package com.example.model"))
        assertTrue(artifact.content.contains("enum class Status"))
        assertTrue(artifact.content.contains("ACTIVE"))
    }

    @Test
    fun `sealed interface render returns source artifact with package-relative path and content`() {
        val generator = KotlinSealedInterfaceGenerator()
        val sealedInterfaceModel =
            GeneratorItem.SealedInterfaceModel(
                name = "Command",
                packageName = "com.example.model",
                description = emptyList(),
            )

        val artifact = generator.render(sealedInterfaceModel)

        assertEquals(GeneratedArtifactKind.SOURCE, artifact.kind)
        assertEquals("com/example/model/Command.kt", artifact.relativePath)
        assertTrue(artifact.content.contains("package com.example.model"))
        assertTrue(artifact.content.contains("sealed interface Command"))
    }

    @Test
    fun `type alias render returns source artifact with package-relative path and content`() {
        val generator = KotlinTypeAliasGenerator()
        val typeAliasModel =
            GeneratorItem.TypeAliasModel(
                name = "UserId",
                packageName = "com.example.model",
                description = emptyList(),
                aliasType = "String",
            )

        val artifact = generator.render(typeAliasModel)

        assertEquals(GeneratedArtifactKind.SOURCE, artifact.kind)
        assertEquals("com/example/model/UserId.kt", artifact.relativePath)
        assertTrue(artifact.content.contains("package com.example.model"))
        assertTrue(artifact.content.contains("typealias UserId = String"))
    }

}
