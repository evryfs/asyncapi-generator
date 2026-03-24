package dev.banking.asyncapi.generator.core.generator.kotlin.headers

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerateHeaderOnlySchemaExclusionTest : AbstractKotlinGeneratorClass() {

    @Test
    fun `should not generate model class for schema used only in message headers`() {
        val modelPackage = "dev.banking.asyncapi.generator.core.model.generated.headerexclusion"
        generateElement(
            yaml = File("src/test/resources/generator/asyncapi_header_only_ref_exclusion.yaml"),
            modelPackage = modelPackage,
            generateModels = true,
        )
        val outputDir = File("target/generated-sources/asyncapi")
        val modelDir = outputDir.resolve(modelPackage.replace('.', '/'))
        assertTrue(modelDir.resolve("GenericMessagePayload.kt").exists(), "Payload model should be generated")
        assertFalse(modelDir.resolve("CommonHeaders.kt").exists(), "Header-only schema should not be generated as model")
    }
}
