package dev.banking.asyncapi.generator.core.generator.avro.oneof

import dev.banking.asyncapi.generator.core.generator.AbstractAvroGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AvroOneOfTest : AbstractAvroGeneratorClass() {

    @Test
    fun `should generate individual records for polymorphic types`() {
        generateAvro(
            yaml = File("src/test/resources/generator/asyncapi_oneof_composition.yaml"),
            packageName = "com.example.poly",
            schema = null
        )

        val outputDir = File("target/generated-sources/asyncapi")
        val packageDir = outputDir.resolve("com/example/poly")

        assertTrue(packageDir.resolve("CardPayment.avsc").exists(), "CardPayment missing")
        assertTrue(packageDir.resolve("BankPayment.avsc").exists(), "BankPayment missing")
        assertFalse(packageDir.resolve("Payment.avsc").exists(), "BankPayment missing")
    }
}
