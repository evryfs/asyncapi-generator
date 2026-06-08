package dev.banking.asyncapi.generator.core.generator.kotlin

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.approvaltests.Approvals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertTrue

class KotlinModelApprovalTest : AbstractKotlinGeneratorClass() {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun approves_generated_kotlin_model() {
        val generated =
            generateElement(
                yaml = File("src/test/resources/generator/asyncapi_simple_transaction_type.yaml"),
                generated = "SimpleTransactionType.kt",
                codegenOutputDirectory = tempDir.resolve("sources").toFile(),
                resourceOutputDirectory = tempDir.resolve("resources").toFile(),
                modelPackage = "dev.banking.asyncapi.generator.core.model.generated.transaction",
            )

        assertTrue(generated.isNotBlank())
        Approvals.verify(generated)
    }
}
