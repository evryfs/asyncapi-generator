package dev.banking.asyncapi.generator.core.generator.avro.array

import dev.banking.asyncapi.generator.core.generator.AbstractAvroGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class AvroArrayTest : AbstractAvroGeneratorClass() {

    @Test
    fun `should generate array types correctly`() {
        val content = generateAvro(
            yaml = File("src/test/resources/generator/asyncapi_array_primitive_object.yaml"),
            packageName = "dev.banking.asyncapi.generator.core.generated.avro",
            schema = "CustomerWithContacts.avsc"
        )
        assertTrue(content.isNotEmpty(), "Generated content should not be empty")
        assertTrue(content.contains("{\"type\": \"array\", \"items\": \"string\"}"), "Missing array of strings")
    }
}
