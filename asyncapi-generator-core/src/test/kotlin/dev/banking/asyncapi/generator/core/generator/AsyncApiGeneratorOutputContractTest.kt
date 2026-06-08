package dev.banking.asyncapi.generator.core.generator

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.BundlerFixtures
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.generator.model.GeneratorOptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AsyncApiGeneratorOutputContractTest {
    private val asyncApiContext = AsyncApiContext()
    private val bundlerFixtures = BundlerFixtures(asyncApiContext)
    private val generator = AsyncApiGenerator()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `generate writes model artifacts to source output directory`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val bundled = bundledDocument()

        generator.generate(
            asyncApiDocument = bundled,
            generatorOptions =
                GeneratorOptions(
                    generatorName = GeneratorName.KOTLIN,
                    modelPackage = "com.example.model",
                    clientPackage = "com.example.client",
                    schemaPackage = "com.example.schema",
                    codegenOutputDirectory = sourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                    generateModels = true,
                    generateAvroSchema = false,
                ),
        )

        assertTrue(sourceOutputDirectory.resolve("com/example/model/Task.kt").exists())
        assertFalse(resourceOutputDirectory.resolve("com/example/model/Task.kt").exists())
    }

    @Test
    fun `generate writes schema artifacts to resource output directory`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val bundled = bundledDocument()

        generator.generate(
            asyncApiDocument = bundled,
            generatorOptions =
                GeneratorOptions(
                    generatorName = GeneratorName.KOTLIN,
                    modelPackage = "com.example.model",
                    clientPackage = "com.example.client",
                    schemaPackage = "com.example.avro",
                    codegenOutputDirectory = sourceOutputDirectory,
                    resourceOutputDirectory = resourceOutputDirectory,
                    generateModels = false,
                    generateAvroSchema = true,
                ),
        )

        assertTrue(resourceOutputDirectory.resolve("com/example/avro/Task.avsc").exists())
        assertFalse(sourceOutputDirectory.resolve("com/example/avro/Task.avsc").exists())
    }

    private fun bundledDocument() =
        bundlerFixtures.bundledDocument(
            File("src/test/resources/generator/asyncapi_enum_default_value.yaml"),
        )
}
