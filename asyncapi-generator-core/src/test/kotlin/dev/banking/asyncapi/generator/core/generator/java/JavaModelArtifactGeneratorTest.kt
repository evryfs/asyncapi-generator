package dev.banking.asyncapi.generator.core.generator.java

import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.java.model.PropertyModel
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JavaModelArtifactGeneratorTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `class render returns source artifact with package-relative path and content`() {
        val generator = JavaClassGenerator(tempDir.toFile(), "com.example.model")
        val classModel =
            GeneratorItem.ClassModel(
                name = "User",
                packageName = "com.example.model",
                description = emptyList(),
                properties =
                    listOf(
                        PropertyModel(
                            name = "id",
                            description = emptyList(),
                            typeName = "String",
                            getterName = "getId",
                            setterName = "setId",
                            annotations = emptyList(),
                        ),
                    ),
            )

        val artifact = generator.render(classModel)

        assertEquals(GeneratedArtifactKind.SOURCE, artifact.kind)
        assertEquals("com/example/model/User.java", artifact.relativePath)
        assertTrue(artifact.content.contains("package com.example.model;"))
        assertTrue(artifact.content.contains("public class User"))
        assertTrue(artifact.content.contains("private String id;"))
    }

    @Test
    fun `class generate writes rendered artifact to output directory`() {
        val generator = JavaClassGenerator(tempDir.toFile(), "com.example.model")
        val classModel =
            GeneratorItem.ClassModel(
                name = "User",
                packageName = "com.example.model",
                description = emptyList(),
                properties =
                    listOf(
                        PropertyModel(
                            name = "id",
                            description = emptyList(),
                            typeName = "String",
                            getterName = "getId",
                            setterName = "setId",
                            annotations = emptyList(),
                        ),
                    ),
            )

        generator.generate(classModel)

        val output = tempDir.resolve("com/example/model/User.java").toFile()
        assertTrue(output.exists())
        assertTrue(output.readText().contains("public class User"))
    }

    @Test
    fun `enum render returns source artifact with package-relative path and content`() {
        val generator = JavaEnumGenerator(tempDir.toFile())
        val enumModel =
            GeneratorItem.EnumModel(
                name = "Status",
                packageName = "com.example.model",
                description = emptyList(),
                values = listOf("ACTIVE", "INACTIVE"),
            )

        val artifact = generator.render(enumModel)

        assertEquals(GeneratedArtifactKind.SOURCE, artifact.kind)
        assertEquals("com/example/model/Status.java", artifact.relativePath)
        assertTrue(artifact.content.contains("package com.example.model;"))
        assertTrue(artifact.content.contains("public enum Status"))
        assertTrue(artifact.content.contains("ACTIVE"))
    }

    @Test
    fun `enum generate writes rendered artifact to output directory`() {
        val generator = JavaEnumGenerator(tempDir.toFile())
        val enumModel =
            GeneratorItem.EnumModel(
                name = "Status",
                packageName = "com.example.model",
                description = emptyList(),
                values = listOf("ACTIVE", "INACTIVE"),
            )

        generator.generate(enumModel)

        val output = tempDir.resolve("com/example/model/Status.java").toFile()
        assertTrue(output.exists())
        assertTrue(output.readText().contains("public enum Status"))
    }

    @Test
    fun `interface render returns source artifact with package-relative path and content`() {
        val generator = JavaInterfaceGenerator(tempDir.toFile())
        val interfaceModel =
            GeneratorItem.InterfaceModel(
                name = "Command",
                packageName = "com.example.model",
                description = emptyList(),
            )

        val artifact = generator.render(interfaceModel)

        assertEquals(GeneratedArtifactKind.SOURCE, artifact.kind)
        assertEquals("com/example/model/Command.java", artifact.relativePath)
        assertTrue(artifact.content.contains("package com.example.model;"))
        assertTrue(artifact.content.contains("public interface Command"))
    }

    @Test
    fun `interface generate writes rendered artifact to output directory`() {
        val generator = JavaInterfaceGenerator(tempDir.toFile())
        val interfaceModel =
            GeneratorItem.InterfaceModel(
                name = "Command",
                packageName = "com.example.model",
                description = emptyList(),
            )

        generator.generate(interfaceModel)

        val output = tempDir.resolve("com/example/model/Command.java").toFile()
        assertTrue(output.exists())
        assertTrue(output.readText().contains("public interface Command"))
    }
}
