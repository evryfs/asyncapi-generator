package dev.banking.asyncapi.generator.core.generator.avro.openapi

import dev.banking.asyncapi.generator.core.generator.AbstractAvroGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class AvroWithOpenapiTest : AbstractAvroGeneratorClass() {

    @Test
    fun `should generate individual records for polymorphic types with ref types from unknown schema`() {
        generateAvro(
            yaml = File("src/test/resources/parser/openapi/asyncapi-single-example.yml"),
            packageName = "com.example.openapi",
            schema = null
        )

        val outputDir = File("target/generated-resources/asyncapi")
        val packageDir = outputDir.resolve("com/example/openapi")

        assertTrue(packageDir.resolve("ActionType.avsc").exists(), "ActionType missing")
        assertTrue(packageDir.resolve("EventMessagePayload.avsc").exists(), "EventMessagePayload missing")
    }

    @Test
    fun `should generate individual records for polymorphic types with nested ref types from unknown schema`() {
        generateAvro(
            yaml = File("src/test/resources/parser/openapi/asyncapi-single-nested-example.yml"),
            packageName = "com.example.openapi",
            schema = null
        )

        val outputDir = File("target/generated-resources/asyncapi")
        val packageDir = outputDir.resolve("com/example/openapi")

        assertTrue(packageDir.resolve("ActionType.avsc").exists(), "ActionType missing")
        assertTrue(packageDir.resolve("EventMessagePayload.avsc").exists(), "EventMessagePayload missing")
        assertTrue(packageDir.resolve("Message.avsc").exists(), "Message missing")
    }

    @Test
    fun `should generate individual records for polymorphic types with allof ref types from unknown schema`() {
        generateAvro(
            yaml = File("src/test/resources/parser/openapi/asyncapi-single-allof-example.yml"),
            packageName = "com.example.openapi",
            schema = null
        )

        val outputDir = File("target/generated-resources/asyncapi")
        val packageDir = outputDir.resolve("com/example/openapi")

        assertTrue(packageDir.resolve("ActionType.avsc").exists(), "ActionType missing")
        assertTrue(packageDir.resolve("EventMessagePayload.avsc").exists(), "EventMessagePayload missing")
        assertTrue(packageDir.resolve("Message.avsc").exists(), "Message missing")
    }

    @Test
    fun `should generate individual records for polymorphic types with nested multiple files ref types from unknown schema`() {
        generateAvro(
            yaml = File("src/test/resources/parser/openapi/asyncapi-single-external-refs-example.yml"),
            packageName = "com.example.openapi",
            schema = null
        )

        val outputDir = File("target/generated-resources/asyncapi")
        val packageDir = outputDir.resolve("com/example/openapi")

        assertTrue(packageDir.resolve("ActionType.avsc").exists(), "ActionType missing")
        assertTrue(packageDir.resolve("EventMessagePayload.avsc").exists(), "EventMessagePayload missing")
        assertTrue(packageDir.resolve("Message.avsc").exists(), "Message missing")
    }
}
