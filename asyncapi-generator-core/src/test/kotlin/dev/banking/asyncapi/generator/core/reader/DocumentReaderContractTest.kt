package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentBoolean
import dev.banking.asyncapi.generator.core.document.DocumentNull
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.fixtures.ReaderFixtures
import dev.banking.asyncapi.generator.core.fixtures.childObject
import dev.banking.asyncapi.generator.core.fixtures.semanticValue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DocumentReaderContractTest {

    private val reader = YamlDocumentReader()
    private val jsonReader = JsonDocumentReader()

    @Test
    fun `reader preserves references without resolving them`() {
        val document = reader.read(ReaderFixtures.yamlSource("reference-preservation.yaml"))
        val root = assertIs<DocumentObject>(document.root)
        val userRef = root.childObject("components")
            .childObject("schemas")
            .childObject("UserRef")
        assertEquals("#/components/schemas/User", userRef["${'$'}ref"]?.semanticValue())
    }

    @Test
    fun `reader accepts object roots without validating AsyncAPI semantics`() {
        val document = reader.read(ReaderFixtures.yamlSource("non-asyncapi-object.yaml"))
        val root = assertIs<DocumentObject>(document.root)
        assertTrue(root.members.containsKey("notAsyncApi"))
        assertTrue(root.members.containsKey("stillReaderInput"))
    }

    @Test
    fun `readers accept scalar roots without applying AsyncAPI rules`() {
        val yamlSource = ReaderFixtures.yamlSource("invalid-root.yaml").copy(content = "true")
        val jsonSource = ReaderFixtures.jsonSource("invalid-root.json").copy(content = "true")

        assertEquals(true, assertIs<DocumentBoolean>(reader.read(yamlSource).root).value)
        assertEquals(true, assertIs<DocumentBoolean>(jsonReader.read(jsonSource).root).value)
    }

    @Test
    fun `readers preserve explicit null roots`() {
        val yamlSource = ReaderFixtures.yamlSource("invalid-root.yaml").copy(content = "null")
        val jsonSource = ReaderFixtures.jsonSource("invalid-root.json").copy(content = "null")

        assertIs<DocumentNull>(reader.read(yamlSource).root)
        assertIs<DocumentNull>(jsonReader.read(jsonSource).root)
    }

    @Test
    fun `yaml and json readers produce equivalent document trees`() {
        val yamlDocument = reader.read(ReaderFixtures.yamlSource("equivalent-document.yaml"))
        val jsonDocument = jsonReader.read(ReaderFixtures.jsonSource("equivalent-document.json"))

        assertEquals(yamlDocument.root.semanticValue(), jsonDocument.root.semanticValue())
    }
}
