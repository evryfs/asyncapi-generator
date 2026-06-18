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
        assertTrue(content.contains("{\"type\": \"array\", \"items\": \"dev.banking.asyncapi.generator.core.generated.avro.ContactPointType\"}"), "Missing array of objects")
    }

    @Test
    fun `should generate array with strings correctly`() {
        val content = generateAvro(
            yaml = File("src/test/resources/generator/asyncapi_array_string.yaml"),
            packageName = "dev.banking.asyncapi.generator.core.generated.avro.string",
            schema = "CustomerWithContacts.avsc"
        )
        assertTrue(content.isNotEmpty(), "Generated content should not be empty")
        assertTrue(content.contains("{\"type\": \"array\", \"items\": \"string\"}"), "Missing array of strings")
    }

    @Test
    fun `should generate array with integers correctly`() {
        val content = generateAvro(
            yaml = File("src/test/resources/generator/asyncapi_array_int.yaml"),
            packageName = "dev.banking.asyncapi.generator.core.generated.avro.integer",
            schema = "CustomerWithContacts.avsc"
        )
        assertTrue(content.isNotEmpty(), "Generated content should not be empty")
        assertTrue(content.contains("{\"type\": \"array\", \"items\": \"int\"}"), "Missing array of strings")
    }
}