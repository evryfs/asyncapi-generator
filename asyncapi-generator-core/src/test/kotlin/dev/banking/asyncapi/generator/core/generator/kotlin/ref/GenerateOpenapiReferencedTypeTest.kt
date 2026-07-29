package dev.banking.asyncapi.generator.core.generator.kotlin.ref

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class GenerateOpenapiReferencedTypeTest : AbstractKotlinGeneratorClass() {
    private val actionTypeClassBody = """
        enum class ActionType {
            CREATE,
            UPDATE,
            DELETE,
        }
    """.trimIndent()

    private val messageClassBody = """
        data class Message(

            val text: String? = null,

            val actionType: ActionType? = null
        ) {
        }
    """.trimIndent()

    @Test
    fun `openapi refs should be completely parsed`() {
        val modelPackage = "dev.banking.asyncapi.generator.core.model.generated.openapi.refs"
        val generated =
            generateElement(
                yaml = File("src/test/resources/parser/openapi/asyncapi-single-example.yml"),
                generated = "EventMessagePayload.kt",
                modelPackage = modelPackage
            )

        val actionTypeGenerated = loadGeneratedClassContent(
            generated = "ActionType.kt",
            modelPackage = modelPackage
        )

        assertTrue(generated.contains("val actionType: ActionType? = null"), "Expected enum ref type for actionType")
        assertEquals(actionTypeClassBody, extractElement(actionTypeGenerated))
    }

    @Test
    fun `openapi external refs with nested objects should be completely parsed`() {
        val modelPackage = "dev.banking.asyncapi.generator.core.model.generated.openapi.refs.nested"
        val generated =
            generateElement(
                yaml = File("src/test/resources/parser/openapi/asyncapi-single-nested-example.yml"),
                generated = "EventMessagePayload.kt",
                modelPackage = modelPackage
            )

        val messageGenerated = loadGeneratedClassContent(
            generated = "Message.kt",
            modelPackage = modelPackage
        )

        val actionTypeGenerated = loadGeneratedClassContent(
            generated = "ActionType.kt",
            modelPackage = modelPackage
        )

        assertTrue(generated.contains("val message: Message? = null"), "Expected complex ref type for message")
        assertEquals(messageClassBody, extractElement(messageGenerated))
        assertEquals(actionTypeClassBody, extractElement(actionTypeGenerated))
    }

    @Test
    fun `openapi allOf directive with external refs should be completely parsed`() {
        val modelPackage = "dev.banking.asyncapi.generator.core.model.generated.openapi.refs.allof"
        val generated =
            generateElement(
                yaml = File("src/test/resources/parser/openapi/asyncapi-single-allof-example.yml"),
                generated = "EventMessagePayload.kt",
                modelPackage = modelPackage
            )

        val messageGenerated = loadGeneratedClassContent(
            generated = "Message.kt",
            modelPackage = modelPackage
        )

        val actionTypeGenerated = loadGeneratedClassContent(
            generated = "ActionType.kt",
            modelPackage = modelPackage
        )

        assertTrue(generated.contains("val message: Message"), "Expected complex ref type for message")
        assertTrue(generated.contains("val entityType: String"), "Expected entityType property")
        assertTrue(generated.contains("val id: UUID"), "Expected id property")
        assertTrue(generated.contains("val initiatedBy: String"), "Expected initiatedBy property")
        assertTrue(generated.contains("val actionType: ActionType"), "Expected actionType property")
        assertTrue(generated.contains("val eventDateTime: OffsetDateTime"), "Expected eventDateTime property")
        assertEquals(messageClassBody, extractElement(messageGenerated))
        assertEquals(actionTypeClassBody, extractElement(actionTypeGenerated))
    }

    @Test
    fun `openapi external refs should be completely parsed`() {
        val modelPackage = "dev.banking.asyncapi.generator.core.model.generated.openapi.refs.external"
        val generated =
            generateElement(
                yaml = File("src/test/resources/parser/openapi/asyncapi-single-external-refs-example.yml"),
                generated = "EventMessagePayload.kt",
                modelPackage = modelPackage
            )

        val messageGenerated = loadGeneratedClassContent(
            generated = "Message.kt",
            modelPackage = modelPackage
        )

        val actionTypeGenerated = loadGeneratedClassContent(
            generated = "ActionType.kt",
            modelPackage = modelPackage
        )

        assertTrue(generated.contains("val message: Message? = null"), "Expected complex ref type for message")
        assertEquals(messageClassBody, extractElement(messageGenerated))
        assertEquals(actionTypeClassBody,  extractElement(actionTypeGenerated))
    }
}
