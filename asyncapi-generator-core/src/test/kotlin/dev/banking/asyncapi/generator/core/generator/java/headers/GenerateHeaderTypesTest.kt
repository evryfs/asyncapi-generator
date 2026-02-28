package dev.banking.asyncapi.generator.core.generator.java.headers

import dev.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class GenerateHeaderTypesTest : AbstractJavaGeneratorClass() {

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
        val headerPath = clientPackage.replace('.', '/') + "/header/TopicUserEventsHeadersUserSignup.java"
        val generated = outputDir.resolve(headerPath).readText()
        assertTrue(generated.contains("class TopicUserEventsHeadersUserSignup"), "Header DTO class missing")
        assertTrue(generated.contains("private String correlationId;"), "Header field correlationId missing")
        assertTrue(generated.contains("private String applicationInstanceId;"), "Header field applicationInstanceId missing")
    }
}
