package dev.banking.asyncapi.generator.core.generator.kotlin

import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.kotlin.model.PropertyModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinGeneratorTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `render returns generation result with artifacts for Kotlin model items`() {
        val generationModel =
            listOf(
                GeneratorItem.DataClassModel(
                    name = "User",
                    packageName = "com.example.model",
                    description = emptyList(),
                    properties =
                        listOf(
                            PropertyModel(
                                name = "id",
                                description = emptyList(),
                                typeName = "String",
                                defaultValue = null,
                                annotations = emptyList(),
                            ),
                        ),
                    parentInterfaces = emptyList(),
                ),
                GeneratorItem.EnumClassModel(
                    name = "Status",
                    packageName = "com.example.model",
                    description = emptyList(),
                    values = listOf("ACTIVE", "INACTIVE"),
                ),
                GeneratorItem.SealedInterfaceModel(
                    name = "Command",
                    packageName = "com.example.model",
                    description = emptyList(),
                ),
                GeneratorItem.TypeAliasModel(
                    name = "UserId",
                    packageName = "com.example.model",
                    description = emptyList(),
                    aliasType = "String",
                ),
                GeneratorItem.KafkaHandlerInterface(
                    name = "IgnoredHandler",
                    packageName = "com.example.kafka",
                    description = emptyList(),
                    methods = emptyList(),
                ),
            )
        val generator =
            KotlinGenerator(
                packageName = "com.example.model",
                outputDir = tempDir.toFile(),
                generationModel = generationModel,
            )

        val result = generator.render()

        assertEquals(
            listOf(
                "com/example/model/User.kt",
                "com/example/model/Status.kt",
                "com/example/model/Command.kt",
                "com/example/model/UserId.kt",
            ),
            result.artifacts.map { it.relativePath },
        )
    }

    @Test
    fun `generate writes rendered artifacts to output directory`() {
        val generationModel =
            listOf(
                GeneratorItem.EnumClassModel(
                    name = "Status",
                    packageName = "com.example.model",
                    description = emptyList(),
                    values = listOf("ACTIVE", "INACTIVE"),
                ),
                GeneratorItem.TypeAliasModel(
                    name = "UserId",
                    packageName = "com.example.model",
                    description = emptyList(),
                    aliasType = "String",
                ),
            )
        val generator =
            KotlinGenerator(
                packageName = "com.example.model",
                outputDir = tempDir.toFile(),
                generationModel = generationModel,
            )

        generator.generate()

        val enumOutput = tempDir.resolve("com/example/model/Status.kt").toFile()
        val typeAliasOutput = tempDir.resolve("com/example/model/UserId.kt").toFile()
        assertTrue(enumOutput.exists())
        assertTrue(typeAliasOutput.exists())
        assertTrue(enumOutput.readText().contains("enum class Status"))
        assertTrue(typeAliasOutput.readText().contains("typealias UserId = String"))
    }
}
