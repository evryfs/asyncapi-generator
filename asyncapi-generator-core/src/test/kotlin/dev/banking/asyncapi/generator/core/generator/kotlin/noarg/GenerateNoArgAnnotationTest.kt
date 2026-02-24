package dev.banking.asyncapi.generator.core.generator.kotlin.noarg

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerateNoArgAnnotationTest : AbstractKotlinGeneratorClass() {

    @Test
    fun `should include no-arg annotation when configured`() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_nullable_types.yaml"),
            generated = "NullableObject.kt",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.noarg",
            configOptions = mapOf(
                "model.annotation" to "com.example.NoArg"
            )
        )
        assertTrue(generated.contains("import com.example.NoArg"))
        assertTrue(generated.contains("@NoArg"))
        assertTrue(generated.contains("data class NullableObject"))
    }

    @Test
    fun `should not include no-arg annotation when not configured`() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_nullable_types.yaml"),
            generated = "NullableObject.kt",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.noarg",
        )
        assertFalse(
            generated.contains("@com.example.NoArg"),
            "NoArg annotation should not be present when not configured"
        )
    }
}
