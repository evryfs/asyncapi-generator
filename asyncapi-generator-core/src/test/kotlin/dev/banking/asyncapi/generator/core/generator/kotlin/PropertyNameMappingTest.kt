package dev.banking.asyncapi.generator.core.generator.kotlin

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class PropertyNameMappingTest : AbstractKotlinGeneratorClass() {

    @Test
    fun `converts kebab-case property names to camelCase with JsonProperty`() {
        val generated = generateElement(
            yaml = yamlWithProperties("event-id", "source-system"),
            generated = "EventPayload.kt",
            modelPackage = "com.example.model",
        )
        assertTrue(generated.contains("val eventId: String"), "kebab-case not converted to camelCase")
        assertTrue(generated.contains("""@field:JsonProperty("event-id")"""), "missing @JsonProperty for kebab-case")
        assertTrue(generated.contains("val sourceSystem: String"), "kebab-case not converted to camelCase")
        assertTrue(generated.contains("""@field:JsonProperty("source-system")"""), "missing @JsonProperty for kebab-case")
    }

    @Test
    fun `preserves valid identifiers without annotation`() {
        val generated = generateElement(
            yaml = yamlWithProperties("userId", "createdAt"),
            generated = "EventPayload.kt",
            modelPackage = "com.example.model",
        )
        assertTrue(generated.contains("val userId: String"), "valid identifier should be preserved")
        assertTrue(generated.contains("val createdAt: String"), "valid identifier should be preserved")
        assertTrue(!generated.contains("@field:JsonProperty"), "valid identifiers should not have @JsonProperty")
    }

    @Test
    fun `prefixes leading digit with underscore`() {
        val generated = generateElement(
            yaml = yamlWithProperties("123value"),
            generated = "EventPayload.kt",
            modelPackage = "com.example.model",
        )
        assertTrue(generated.contains("val _123value: String"), "leading digit should be prefixed with underscore")
        assertTrue(generated.contains("""@field:JsonProperty("123value")"""), "missing @JsonProperty for leading digit")
    }

    @Test
    fun `suffixes reserved words with underscore`() {
        val generated = generateElement(
            yaml = yamlWithProperties("class", "when"),
            generated = "EventPayload.kt",
            modelPackage = "com.example.model",
        )
        assertTrue(generated.contains("val class_: String"), "reserved word should be suffixed")
        assertTrue(generated.contains("val when_: String"), "reserved word should be suffixed")
        assertTrue(generated.contains("""@field:JsonProperty("class")"""), "missing @JsonProperty for reserved word")
        assertTrue(generated.contains("""@field:JsonProperty("when")"""), "missing @JsonProperty for reserved word")
    }

    @Test
    fun `preserves snake_case identifiers`() {
        val generated = generateElement(
            yaml = yamlWithProperties("user_id", "created_at"),
            generated = "EventPayload.kt",
            modelPackage = "com.example.model",
        )
        assertTrue(generated.contains("val user_id: String"), "snake_case should be preserved")
        assertTrue(generated.contains("val created_at: String"), "snake_case should be preserved")
        assertTrue(!generated.contains("@field:JsonProperty"), "snake_case should not have @JsonProperty")
    }

    private fun yamlWithProperties(vararg propertyNames: String): File {
        val properties = propertyNames.joinToString("\n") { name ->
            "          $name:\n            type: string"
        }
        val yamlContent = """
asyncapi: '3.0.0'
info:
  title: Test
  version: '1.0.0'
components:
  schemas:
    EventPayload:
      type: object
      properties:
$properties
        """.trimIndent()
        val yamlFile = File("target/test-output/property-mapping/test.yaml")
        yamlFile.parentFile.mkdirs()
        yamlFile.writeText(yamlContent)
        return yamlFile
    }
}
