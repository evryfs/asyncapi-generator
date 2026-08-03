package dev.banking.asyncapi.generator.core.registry

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AsyncApiRegistryTest {

    @Test
    fun `read creates parser root through document reader stage`() {
        val context = AsyncApiContext()
        val file = TestResources.file("reader/yaml/source-map.yaml")
        val root = AsyncApiRegistry.read(file, context)
        assertEquals("source_map.root", root.name)
        assertEquals("source_map.root", root.path)
        assertEquals("3.0.0", root.expectObject().optional("asyncapi")?.expect<String>())
        assertEquals(1, context.sourceRepository.getLine("source_map.root"))
        assertEquals(2, context.sourceRepository.getLine("source_map.root.info"))
        assertEquals(3, context.sourceRepository.getLine("source_map.root.info.title"))
        assertEquals(5, context.sourceRepository.getLine("source_map.root.info.tags[0]"))
    }

    @Test
    fun `read supports json input`() {
        val context = AsyncApiContext()
        val file = TestResources.file("reader/json/source-map.json")
        val root = AsyncApiRegistry.read(file, context)
        assertEquals("source_map.root", root.name)
        assertEquals("source_map.root", root.path)
        assertEquals("3.0.0", root.expectObject().optional("asyncapi")?.expect<String>())
        assertEquals(1, context.sourceRepository.getLine("source_map.root"))
        assertEquals(3, context.sourceRepository.getLine("source_map.root.info"))
        assertEquals(4, context.sourceRepository.getLine("source_map.root.info.title"))
        assertEquals(6, context.sourceRepository.getLine("source_map.root.info.tags[0]"))
    }
}
