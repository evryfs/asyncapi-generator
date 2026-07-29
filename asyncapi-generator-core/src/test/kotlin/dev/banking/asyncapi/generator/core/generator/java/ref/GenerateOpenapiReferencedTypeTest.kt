package dev.banking.asyncapi.generator.core.generator.java.ref

import dev.banking.asyncapi.generator.core.generator.AbstractJavaGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class GenerateOpenapiReferencedTypeTest : AbstractJavaGeneratorClass() {
    private val actionTypeClassBody = """
            public enum ActionType implements Serializable {
                CREATE,
                UPDATE,
                DELETE,
            }
        """.trimIndent()

    private val messageClassBody = """
            public class Message implements Serializable {

                private String text;

                private ActionType actionType;

                public Message() {
                    // Default constructor
                }

                // All-args constructor
                public Message(
                    String text,
                    ActionType actionType
                ) {
                    this.text = text;
                    this.actionType = actionType;
                }

                /**
                 * Get text.
                 * @return String
                 */
                public String getText() {
                    return text;
                }

                /**
                 * Set text.
                 * @param text
                 */
                public void setText(String text) {
                    this.text = text;
                }

                /**
                 * Get actionType.
                 * @return ActionType
                 */
                public ActionType getActionType() {
                    return actionType;
                }

                /**
                 * Set actionType.
                 * @param actionType
                 */
                public void setActionType(ActionType actionType) {
                    this.actionType = actionType;
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    Message that = (Message) o;
                    return
                        Objects.equals(text, that.text) &&

                        Objects.equals(actionType, that.actionType)
            ;
                }

                @Override
                public int hashCode() {
                    return Objects.hash(

                        text,
                        actionType
                    );
                }

                @Override
                public String toString() {
                    StringBuilder sb = new StringBuilder();
                    sb.append("class Message {\n");
                    sb.append("    text: ").append(text).append("\n");
                    sb.append("    actionType: ").append(actionType).append("\n");
                    sb.append("}");
                    return sb.toString();
                }
            }
        """.trimIndent()

    @Test
    fun `openapi refs should be completely parsed`() {
        val modelPackage = "dev.banking.asyncapi.generator.core.model.generated.openapi.refs"
        val generated =
            generateElement(
                yaml = File("src/test/resources/parser/openapi/asyncapi-single-example.yml"),
                generated = "EventMessagePayload.java",
                modelPackage = modelPackage
            )

        val actionTypeGenerated = loadGeneratedClassContent(
            generated = "ActionType.java",
            modelPackage = modelPackage
        )

        assertTrue(generated.contains("private ActionType actionType;"), "Expected enum ref type for actionType")
        assertEquals(actionTypeClassBody, extractClassBody(actionTypeGenerated))
    }

    @Test
    fun `openapi external refs with nested objects should be completely parsed`() {
        val modelPackage = "dev.banking.asyncapi.generator.core.model.generated.openapi.refs.nested"
        val generated =
            generateElement(
                yaml = File("src/test/resources/parser/openapi/asyncapi-single-nested-example.yml"),
                generated = "EventMessagePayload.java",
                modelPackage = modelPackage
            )

        val messageGenerated = loadGeneratedClassContent(
            generated = "Message.java",
            modelPackage = modelPackage
        )

        val actionTypeGenerated = loadGeneratedClassContent(
            generated = "ActionType.java",
            modelPackage = modelPackage
        )

        assertTrue(generated.contains("private Message message;"), "Expected complex ref type for message")
        assertEquals(messageClassBody, extractClassBody(messageGenerated))
        assertEquals(actionTypeClassBody, extractClassBody(actionTypeGenerated))
    }

    @Test
    fun `openapi allOf directive with external refs should be completely parsed`() {
        val modelPackage = "dev.banking.asyncapi.generator.core.model.generated.openapi.refs.allof"
        val generated =
            generateElement(
                yaml = File("src/test/resources/parser/openapi/asyncapi-single-allof-example.yml"),
                generated = "EventMessagePayload.java",
                modelPackage = modelPackage
            )

        val messageGenerated = loadGeneratedClassContent(
            generated = "Message.java",
            modelPackage = modelPackage
        )

        val actionTypeGenerated = loadGeneratedClassContent(
            generated = "ActionType.java",
            modelPackage = modelPackage
        )

        assertTrue(generated.contains("private Message message;"), "Expected complex ref type for message")
        assertTrue(generated.contains("private String entityType;"), "Expected entityType property")
        assertTrue(generated.contains("private UUID id;"), "Expected id property")
        assertTrue(generated.contains("private String initiatedBy;"), "Expected initiatedBy property")
        assertTrue(generated.contains("private ActionType actionType;"), "Expected actionType property")
        assertTrue(generated.contains("private OffsetDateTime eventDateTime;"), "Expected eventDateTime property")
        assertEquals(messageClassBody, extractClassBody(messageGenerated))
        assertEquals(actionTypeClassBody, extractClassBody(actionTypeGenerated))
    }

    @Test
    fun `openapi external refs should be completely parsed`() {
        val modelPackage = "dev.banking.asyncapi.generator.core.model.generated.openapi.refs.external"
        val generated =
            generateElement(
                yaml = File("src/test/resources/parser/openapi/asyncapi-single-external-refs-example.yml"),
                generated = "EventMessagePayload.java",
                modelPackage = modelPackage
            )

        val messageGenerated = loadGeneratedClassContent(
            generated = "Message.java",
            modelPackage = modelPackage
        )

        val actionTypeGenerated = loadGeneratedClassContent(
            generated = "ActionType.java",
            modelPackage = modelPackage
        )

        assertTrue(generated.contains("private Message message;"), "Expected complex ref type for message")
        assertEquals(messageClassBody, extractClassBody(messageGenerated))
        assertEquals(actionTypeClassBody,  extractClassBody(actionTypeGenerated))
    }
}
