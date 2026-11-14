package com.example.bundling

import com.fasterxml.jackson.databind.JsonNode
import systemtest.support.Documents
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BundlingTest {
    private val bundles = listOf(
        "bundles/channel-fragment.yaml",
        "bundles/schema-fragment.yaml",
        "bundles/separate-directory.yaml",
        "bundles/single-file.yaml",
        "bundles/nested/directories/single-file.yaml",
    )

    @Test
    fun `every bundle is a self-contained AsyncAPI document`() {
        bundles.forEach { path ->
            val document = Documents.read(path)
            assertEquals("3.0.0", document.path("asyncapi").asText(), "$path declares the AsyncAPI version")
            assertEquals(emptyList(), Documents.externalReferences(document), "$path has no external references")
        }
    }

    @Test
    fun `bundles contain no generator-internal fields`() {
        bundles.forEach { path ->
            assertFalse("defaultSet" in Documents.fieldNames(Documents.read(path)), "$path leaks 'defaultSet'")
        }
    }

    @Test
    fun `an external channel and its nested schema fragment are inlined`() {
        val document = Documents.read("bundles/channel-fragment.yaml")
        assertEquals("example.topic", document.at("/channels/testChannel/address").asText())
        assertDescribesUserId(document)
    }

    @Test
    fun `external property fragments are inlined from the same and from another directory`() {
        listOf("bundles/schema-fragment.yaml", "bundles/separate-directory.yaml").forEach { path ->
            val document = Documents.read(path)
            assertFalse(document.at("/channels/schemaFragmentChannel").isMissingNode, "$path keeps its channel")
            assertDescribesUserId(document)
        }
    }

    @Test
    fun `the asyncapi-yaml profile and a code profile write the same bundle`() {
        assertEquals(
            Documents.read("bundles/single-file.yaml"),
            Documents.read("bundles/nested/directories/single-file.yaml"),
        )
    }

    private fun assertDescribesUserId(document: JsonNode) {
        assertTrue(
            "The unique identifier of the user" in document.findValuesAsText("description"),
            "The external 'id' property fragment is inlined",
        )
    }
}
