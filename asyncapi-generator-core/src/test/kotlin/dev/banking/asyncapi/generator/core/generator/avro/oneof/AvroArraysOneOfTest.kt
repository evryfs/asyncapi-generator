package dev.banking.asyncapi.generator.core.generator.avro.oneof

import dev.banking.asyncapi.generator.core.generator.AbstractAvroGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AvroArraysOneOfTest : AbstractAvroGeneratorClass() {

    @Test
    fun `should generate individual records for polymorphic types`() {
        generateAvro(
            yaml = File("src/test/resources/generator/asyncapi_oneof_arrays_composition.yaml"),
            packageName = "com.example.poly.arrays",
            schema = null
        )

        val outputDir = File("target/generated-resources/asyncapi")
        val packageDir = outputDir.resolve("com/example/poly/arrays")

        assertTrue(packageDir.resolve("Payment.avsc").exists(), "Payment missing")
        assertTrue(packageDir.resolve("PaymentBulk.avsc").exists(), "PaymentBulk missing")
        assertFalse(packageDir.resolve("PaymentOperation.avsc").exists(), "PaymentOperation missing")
    }
}
