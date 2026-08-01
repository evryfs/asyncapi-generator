package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.fixtures.ReaderFixtures
import dev.banking.asyncapi.generator.core.fixtures.childObject
import dev.banking.asyncapi.generator.core.fixtures.semanticValue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DocumentReaderContractTest {

    private val reader = YamlDocumentReader()
    private val jsonReader = JsonDocumentReader()

    @Test
    fun `reader preserves references without resolving them`() {
        val document = reader.read(ReaderFixtures.yamlSource("reference-preservation.yaml"))
        val userRef = document.root.childObject("components")
            .childObject("schemas")
            .childObject("UserRef")
        assertEquals("#/components/schemas/User", userRef["${'$'}ref"]?.semanticValue())
    }

    @Test
    fun `reader accepts object roots without validating AsyncAPI semantics`() {
        val document = reader.read(ReaderFixtures.yamlSource("non-asyncapi-object.yaml"))
        assertTrue(document.root.members.containsKey("notAsyncApi"))
        assertTrue(document.root.members.containsKey("stillReaderInput"))
    }

    @Test
    fun `yaml and json readers produce equivalent document trees`() {
        val yamlDocument = reader.read(ReaderFixtures.yamlSource("equivalent-document.yaml"))
        val jsonDocument = jsonReader.read(ReaderFixtures.jsonSource("equivalent-document.json"))

        assertEquals(yamlDocument.root.semanticValue(), jsonDocument.root.semanticValue())
    }
}
