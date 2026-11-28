package dev.banking.asyncapi.generator.core.generator.java

import dev.banking.asyncapi.generator.core.fixtures.GeneratorApprovalFormat
import dev.banking.asyncapi.generator.core.fixtures.GeneratorApprovals
import dev.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import dev.banking.asyncapi.generator.core.generator.configuration.ModelType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertTrue

internal class JavaModelApprovalTest : AbstractJavaGeneratorClass() {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun approves_generated_java_model() {
        val generated =
            generateElement(
                yaml = File("src/test/resources/generator/asyncapi_simple_transaction_type.yaml"),
                generated = "SimpleTransactionType.java",
                codegenOutputDirectory = tempDir.resolve("sources").toFile(),
                modelPackage = "dev.banking.asyncapi.generator.core.model.generated.transaction",
            )

        assertTrue(generated.isNotBlank())
        GeneratorApprovals.verify(
            generated = generated,
            format = GeneratorApprovalFormat.JAVA,
            scenario = "simple-transaction-class",
        )
    }

    @Test
    fun approves_generated_java_record_model() {
        val generated =
            generateElement(
                yaml = File("src/test/resources/generator/asyncapi_simple_transaction_type.yaml"),
                generated = "SimpleTransactionType.java",
                codegenOutputDirectory = tempDir.resolve("record-sources").toFile(),
                modelPackage = "dev.banking.asyncapi.generator.core.model.generated.transaction",
                modelType = ModelType.JAVA_RECORD,
            )

        assertTrue(generated.isNotBlank())
        GeneratorApprovals.verify(
            generated = generated,
            format = GeneratorApprovalFormat.JAVA,
            scenario = "simple-transaction-record",
        )
    }
}
