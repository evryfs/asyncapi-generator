package dev.banking.asyncapi.generator.core.generator.kotlin

import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinModelArtifactGeneratorTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `enum render returns source artifact with package-relative path and content`() {
        val generator = KotlinEnumGenerator(tempDir.toFile())
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
    fun `enum generate writes rendered artifact to output directory`() {
        val generator = KotlinEnumGenerator(tempDir.toFile())
        val enumModel =
            GeneratorItem.EnumClassModel(
                name = "Status",
                packageName = "com.example.model",
                description = emptyList(),
                values = listOf("ACTIVE", "INACTIVE"),
            )

        generator.generate(enumModel)

        val output = tempDir.resolve("com/example/model/Status.kt").toFile()
        assertTrue(output.exists())
        assertTrue(output.readText().contains("enum class Status"))
    }

    @Test
    fun `sealed interface render returns source artifact with package-relative path and content`() {
        val generator = KotlinSealedInterfaceGenerator(tempDir.toFile())
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
    fun `sealed interface generate writes rendered artifact to output directory`() {
        val generator = KotlinSealedInterfaceGenerator(tempDir.toFile())
        val sealedInterfaceModel =
            GeneratorItem.SealedInterfaceModel(
                name = "Command",
                packageName = "com.example.model",
                description = emptyList(),
            )

        generator.generate(sealedInterfaceModel)

        val output = tempDir.resolve("com/example/model/Command.kt").toFile()
        assertTrue(output.exists())
        assertTrue(output.readText().contains("sealed interface Command"))
    }

    @Test
    fun `type alias render returns source artifact with package-relative path and content`() {
        val generator = KotlinTypeAliasGenerator(tempDir.toFile())
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

    @Test
    fun `type alias generate writes rendered artifact to output directory`() {
        val generator = KotlinTypeAliasGenerator(tempDir.toFile())
        val typeAliasModel =
            GeneratorItem.TypeAliasModel(
                name = "UserId",
                packageName = "com.example.model",
                description = emptyList(),
                aliasType = "String",
            )

        generator.generate(typeAliasModel)

        val output = tempDir.resolve("com/example/model/UserId.kt").toFile()
        assertTrue(output.exists())
        assertTrue(output.readText().contains("typealias UserId = String"))
    }
}
