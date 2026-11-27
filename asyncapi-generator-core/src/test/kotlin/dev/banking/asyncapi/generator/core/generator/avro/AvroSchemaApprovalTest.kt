package dev.banking.asyncapi.generator.core.generator.avro

import dev.banking.asyncapi.generator.core.fixtures.GeneratorApprovalFormat
import dev.banking.asyncapi.generator.core.fixtures.GeneratorApprovals
import dev.banking.asyncapi.generator.core.generator.AbstractAvroGeneratorClass
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertTrue

internal class AvroSchemaApprovalTest : AbstractAvroGeneratorClass() {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun approves_generated_avro_schema() {
        val generated =
            generateAvro(
                yaml = File("src/test/resources/generator/asyncapi_enum_default_value.yaml"),
                codegenOutputDirectory = tempDir.resolve("sources").toFile(),
                packageName = "com.example.avro",
                schema = "Task.avsc",
            )

        assertTrue(generated.isNotBlank())
        GeneratorApprovals.verify(
            generated = generated,
            format = GeneratorApprovalFormat.AVRO,
            scenario = "task-schema",
        )
    }
}
