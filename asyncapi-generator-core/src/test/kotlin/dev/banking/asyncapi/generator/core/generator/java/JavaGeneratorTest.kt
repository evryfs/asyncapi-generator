package dev.banking.asyncapi.generator.core.generator.java

import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.java.model.PropertyModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JavaGeneratorTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `render returns generation result with artifacts for Java model items`() {
        val generationModel =
            listOf(
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
                ),
                GeneratorItem.EnumModel(
                    name = "Status",
                    packageName = "com.example.model",
                    description = emptyList(),
                    values = listOf("ACTIVE", "INACTIVE"),
                ),
                GeneratorItem.InterfaceModel(
                    name = "Command",
                    packageName = "com.example.model",
                    description = emptyList(),
                ),
                GeneratorItem.KafkaHandlerInterface(
                    name = "IgnoredHandler",
                    packageName = "com.example.kafka",
                    description = emptyList(),
                    methods = emptyList(),
                ),
            )
        val generator =
            JavaGenerator(
                packageName = "com.example.model",
                outputDir = tempDir.toFile(),
                generationModel = generationModel,
            )

        val result = generator.render()

        assertEquals(
            listOf(
                "com/example/model/User.java",
                "com/example/model/Status.java",
                "com/example/model/Command.java",
            ),
            result.artifacts.map { it.relativePath },
        )
    }

    @Test
    fun `generate writes rendered artifacts to output directory`() {
        val generationModel =
            listOf(
                GeneratorItem.EnumModel(
                    name = "Status",
                    packageName = "com.example.model",
                    description = emptyList(),
                    values = listOf("ACTIVE", "INACTIVE"),
                ),
                GeneratorItem.InterfaceModel(
                    name = "Command",
                    packageName = "com.example.model",
                    description = emptyList(),
                ),
            )
        val generator =
            JavaGenerator(
                packageName = "com.example.model",
                outputDir = tempDir.toFile(),
                generationModel = generationModel,
            )

        generator.generate()

        val enumOutput = tempDir.resolve("com/example/model/Status.java").toFile()
        val interfaceOutput = tempDir.resolve("com/example/model/Command.java").toFile()
        assertTrue(enumOutput.exists())
        assertTrue(interfaceOutput.exists())
        assertTrue(enumOutput.readText().contains("public enum Status"))
        assertTrue(interfaceOutput.readText().contains("public interface Command"))
    }
}
