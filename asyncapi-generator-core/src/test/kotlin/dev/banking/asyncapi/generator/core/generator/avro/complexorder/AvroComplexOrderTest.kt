package dev.banking.asyncapi.generator.core.generator.avro.complexorder

import dev.banking.asyncapi.generator.core.generator.AbstractAvroGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class AvroComplexOrderTest : AbstractAvroGeneratorClass() {

    @Test
    fun `should generate complex order schema`() {
        val content = generateAvro(
            yaml = File("src/test/resources/generator/asyncapi_complex_order_payload_type.yaml"),
            packageName = "dev.banking.asyncapi.generator.core.generated.avro",
            schema = "CustomerWithContacts.avsc"
        )

        assertTrue(content.isNotEmpty(), "Generated content should not be empty")
        assertTrue(content.contains("logicalType\": \"uuid\""), "UUID missing")
        assertTrue(content.contains("{\"type\": \"array\", \"items\": \"string\"}"), "Tags array missing")
        assertTrue(content.contains("contactPoints"), "contactPoints field missing")
    }

    @Test
    fun `should generate referenced types as named types`() {
        val content = generateAvro(
            yaml = File("src/test/resources/generator/asyncapi_complex_order_payload_type.yaml"),
            packageName = "dev.banking.avro",
            schema = "ComplexOrderPayloadType.avsc"
        )

        assertTrue(content.isNotEmpty(), "Generated content empty")
        assertTrue(content.contains("\"namespace\": \"dev.banking.avro\""))
        assertTrue(
            content.contains("\"type\": \"dev.banking.avro.CustomerWithContacts\""),
            "Customer reference missing or incorrect"
        )
        assertTrue(content.contains("{\"type\": \"array\", \"items\": \"string\"}"), "Array should default to string items for now")
    }

    @Test
    fun `should generate all referenced schemas as separate files`() {
        generateAvro(
            yaml = File("src/test/resources/generator/asyncapi_complex_order_payload_type.yaml"),
            packageName = "com.example.avro",
            schema = null
        )

        val packagePath = "com/example/avro"
        val outputPath =  File("target/generated-resources/asyncapi")
        val packageDir = outputPath.resolve(packagePath)

        assertTrue(packageDir.resolve("ComplexOrderPayloadType.avsc").exists(), "Main schema missing")
        assertTrue(packageDir.resolve("CustomerWithContacts.avsc").exists(), "Referenced Customer schema missing")
        assertTrue(packageDir.resolve("ContactPointType.avsc").exists(), "Transitive reference ContactPointType missing")
        assertTrue(packageDir.resolve("OrderLineType.avsc").exists(), "Referenced OrderLineType schema missing")
    }
}
