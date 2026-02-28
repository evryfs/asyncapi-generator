package dev.banking.asyncapi.generator.core.generator.kotlin.headers

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class GenerateHeaderTypesTest : AbstractKotlinGeneratorClass() {

    @Test
    fun `should generate header DTO for message headers`() {
        val yaml = File("src/test/resources/generator/asyncapi_message_headers.yaml")
        val clientPackage = "dev.banking.asyncapi.generator.core.client"
        generateElement(
            yaml = yaml,
            generated = null,
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.headers",
            clientPackage = clientPackage,
            generateSpringKafkaClient = true,
            generateModels = false,
        )
        val outputDir = File("target/generated-sources/asyncapi")
        val headerPath = clientPackage.replace('.', '/') + "/header/TopicUserEventsHeadersUserSignup.kt"
        val generated = outputDir.resolve(headerPath).readText()
        assertTrue(generated.contains("data class TopicUserEventsHeadersUserSignup"), "Header DTO class missing")
        assertTrue(generated.contains("val correlationId: String"), "Header field correlationId missing")
        assertTrue(
            generated.contains("val applicationInstanceId: String"),
            "Header field applicationInstanceId missing"
        )
    }
}
