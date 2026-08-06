package dev.banking.asyncapi.generator.core.generator.java

import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.java.model.PropertyModel
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JavaModelArtifactGeneratorTest {
    @Test
    fun `class render returns source artifact with package-relative path and content`() {
        val generator = JavaClassGenerator("com.example.model")
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
    fun `record render returns source artifact with package-relative path and content`() {
        val generator = JavaRecordGenerator("com.example.model")
        val classModel =
            GeneratorItem.ClassModel(
                name = "User",
                packageName = "com.example.model",
                description = emptyList(),
                properties =
                    listOf(
                        PropertyModel(
                            name = "id",
                            description = listOf("User identifier."),
                            typeName = "String",
                            getterName = "getId",
                            setterName = "setId",
                            annotations = listOf("@NotNull"),
                        ),
                    ),
                implementsInterfaces = listOf("Serializable"),
            )

        val artifact = generator.render(classModel)

        assertEquals(GeneratedArtifactKind.SOURCE, artifact.kind)
        assertEquals("com/example/model/User.java", artifact.relativePath)
        assertTrue(artifact.content.contains("package com.example.model;"))
        assertTrue(artifact.content.contains("public record User("))
        assertTrue(artifact.content.contains("@param id User identifier."))
        assertTrue(artifact.content.contains("@NotNull"))
        assertTrue(artifact.content.contains("String id"))
        assertTrue(artifact.content.contains("implements Serializable"))
        assertTrue(!artifact.content.contains("java.util.Objects"))
        assertTrue(!artifact.content.contains("void setId"))
    }

    @Test
    fun `record render uses compact component list for model without properties`() {
        val generator = JavaRecordGenerator("com.example.model")
        val classModel =
            GeneratorItem.ClassModel(
                name = "EmptyPayload",
                packageName = "com.example.model",
                description = emptyList(),
                properties = emptyList(),
                implementsInterfaces = listOf("Serializable"),
            )

        val artifact = generator.render(classModel)

        assertTrue(artifact.content.contains("public record EmptyPayload() implements Serializable"))
    }

    @Test
    fun `enum render returns source artifact with package-relative path and content`() {
        val generator = JavaEnumGenerator()
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
    fun `interface render returns source artifact with package-relative path and content`() {
        val generator = JavaInterfaceGenerator()
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

}
