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
                "model.noArgAnnotation" to "com.example.NoArg"
            )
        )
        assertTrue(
            generated.contains("@com.example.NoArg"),
            "Expected NoArg annotation to be present in generated Kotlin Data Class"
        )
        assertTrue(
            generated.contains("data class NullableObject"),
            "Expected data class declaration to be present"
        )
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
