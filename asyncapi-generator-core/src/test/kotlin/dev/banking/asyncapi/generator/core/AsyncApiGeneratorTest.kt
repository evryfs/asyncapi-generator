package dev.banking.asyncapi.generator.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AsyncApiGeneratorTest {
    @TempDir
    lateinit var outputDirectory: File

    private val generator = AsyncApiGenerator()

    @Test
    fun `configure tells what a generation writes and whether it runs protoc`() {
        val models = generator.configure(settings(generatorName = "kotlin", modelPackage = "com.example.model"))
        val schemas = generator.configure(settings(generatorName = "avro-schema", schemaPackage = "com.example.avro"))
        val protobufModels =
            generator.configure(
                settings(
                    generatorName = "java",
                    modelPackage = "com.example.proto",
                    modelConfig = GeneratorSettings.ModelConfig(modelType = "protobuf-message"),
                ),
            )

        assertEquals(
            listOf(true, false, false),
            listOf(models.writesSources, models.writesSchemas, models.requiresProtoc),
        )
        assertEquals(
            listOf(false, true, false),
            listOf(schemas.writesSources, schemas.writesSchemas, schemas.requiresProtoc),
        )
        assertTrue(protobufModels.requiresProtoc)
    }

    @Test
    fun `an invalid setting fails before the contract is read, named as users write it`() {
        val failure =
            assertThrows<AsyncApiGenerationException.InvalidSettings> {
                generator.configure(settings(generatorName = "cobol", modelPackage = "com.example.model"))
            }

        assertTrue("generatorName" in failure.message.orEmpty(), failure.message)
    }

    @Test
    fun `generation writes the configured output and reports a missing contract as a failure`() {
        val generation = generator.configure(settings(generatorName = "kotlin", modelPackage = "com.example.model"))
        val contract = File("src/test/resources/asyncapi_kafka_single_file_example.yaml")

        generator.generate(contract, generation) {}

        assertTrue(outputDirectory.walk().any { it.extension == "kt" })
        assertFalse(generator.inputFiles(contract).isEmpty())
        assertThrows<AsyncApiGenerationException.Failed> {
            generator.generate(File("missing.yaml"), generation) {}
        }
    }

    private fun settings(
        generatorName: String,
        modelPackage: String? = null,
        schemaPackage: String? = null,
        modelConfig: GeneratorSettings.ModelConfig? = null,
    ) = GeneratorSettings(
        generatorName = generatorName,
        outputDirectory = outputDirectory,
        modelPackage = modelPackage,
        schemaPackage = schemaPackage,
        modelConfig = modelConfig,
    )
}
