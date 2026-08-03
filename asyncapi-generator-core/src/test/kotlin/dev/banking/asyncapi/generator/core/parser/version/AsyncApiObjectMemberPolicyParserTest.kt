package dev.banking.asyncapi.generator.core.parser.version

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentSource
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AsyncApiObjectMemberPolicyParserTest {

    @Test
    fun `version 3 profile defines every ordinary object policy`() {
        val policies = AsyncApiObjectType.entries.associateWith(AsyncApiParserProfile.V3_0::objectMemberPolicy)

        assertEquals(AsyncApiObjectType.entries.toSet(), policies.keys)
        assertTrue(policies.values.all { it.allowedMembers.isNotEmpty() })
        assertTrue(policies.values.all { it.specificationExtensionsAllowed })
    }

    @Test
    fun `version 3 profile keeps patterned and free form objects outside fixed member policies`() {
        val fixedObjectNames = AsyncApiObjectType.entries.map(AsyncApiObjectType::displayName)

        assertTrue("Schema Object" !in fixedObjectNames)
        assertTrue("Reference Object" !in fixedObjectNames)
        assertTrue("Bindings Object" !in fixedObjectNames)
        assertTrue("Channels Object" !in fixedObjectNames)
    }

    @Test
    fun `every ordinary object policy accepts extensions and rejects unknown members`() {
        AsyncApiObjectType.entries.forEach { objectType ->
            parserObject("x-owner: team").expectOnlyMembers(objectType)

            val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure>(objectType.displayName) {
                parserObject("unexpected: true").expectOnlyMembers(objectType)
            }
            val diagnostic = assertIs<ParserDiagnostic.UnexpectedObjectMember>(error.diagnostic)

            assertEquals(objectType.displayName, diagnostic.objectType)
            assertEquals("unexpected", diagnostic.memberName)
            assertEquals("asyncapi.root.unexpected", diagnostic.path)
        }
    }

    private fun parserObject(content: String) =
        ParserNodeFactory.root(
            document = DocumentReaderRegistry.read(
                DocumentSource(
                    id = "asyncapi",
                    file = File("asyncapi.yaml").canonicalFile,
                    content = content,
                    format = DocumentFormat.YAML,
                ),
            ),
            context = AsyncApiContext(),
        ).withProfile(AsyncApiParserProfile.V3_0).expectObject()
}
