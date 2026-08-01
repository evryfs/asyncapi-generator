package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import dev.banking.asyncapi.generator.core.parser.schemas.SchemaParser
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiParserProfile
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class ExternalReferenceLoadingTest : ParserTestSupport() {
    private val parser = SchemaParser(asyncApiContext)

    @Test
    fun `loads escaped pointers root schemas and equivalent yaml and json fragments`() {
        val schemas = parseValidReferences()

        assertEquals(
            "escaped pointer target",
            resolvedSchema(schemas, "EscapedReference").description,
        )
        assertEquals(
            "root schema target",
            resolvedSchema(schemas, "RootReference").description,
        )
        assertEquals(
            resolvedSchema(schemas, "YamlReference").description,
            resolvedSchema(schemas, "JsonReference").description,
        )
        assertEquals(
            "percent encoded URI and pointer",
            resolvedSchema(schemas, "EncodedReference").description,
        )
        assertEquals(
            true,
            resolvedBooleanSchema(schemas, "BooleanRootReference").value,
        )
        assertEquals(
            "array root target",
            resolvedSchema(schemas, "ArrayRootReference").description,
        )
    }

    @Test
    fun `keeps same named external documents in different directories distinct`() {
        val schemas = parseValidReferences()
        val left = resolvedSchema(schemas, "LeftReference")
        val right = resolvedSchema(schemas, "RightReference")

        assertEquals("left shared schema", left.description)
        assertEquals("right shared schema", right.description)

        val leftLocation = assertNotNull(asyncApiContext.getSourceLocation(left))
        val rightLocation = assertNotNull(asyncApiContext.getSourceLocation(right))
        assertEquals("shared.yaml", leftLocation.file.name)
        assertEquals("shared.yaml", rightLocation.file.name)
        assertNotEquals(leftLocation.file.canonicalPath, rightLocation.file.canonicalPath)
        assertNotEquals(
            asyncApiContext.sourceRepository.findIdByFile(leftLocation.file),
            asyncApiContext.sourceRepository.findIdByFile(rightLocation.file),
        )
    }

    @Test
    fun `dispatches a standalone external fragment using its reference category`() {
        parseDocument("parser/references/external/message-main.yaml")

        val reference = assertIs<Reference>(
            asyncApiContext.modelRepository.getModelsByPath()[
                "message_main.root.components.messages.ExternalEvent"
            ],
        )
        assertEquals(
            AsyncApiParserProfile.V3_0,
            asyncApiContext.modelRepository.getReferenceOrigin(reference)?.parserProfile,
        )
        val message = assertNotNull(
            asyncApiContext.modelRepository.getModelsByPath()["messages.root.ExternalEvent"],
        )
        assertEquals("messages.yaml", asyncApiContext.getSourceLocation(message)?.file?.name)
    }

    @Test
    fun `loads a whole file containing one message object`() {
        parseDocument("parser/references/external/message-root-main.yaml")

        val reference = assertIs<Reference>(
            asyncApiContext.modelRepository.getModelsByPath()[
                "message_root_main.root.components.messages.RootEvent"
            ],
        )
        val message = assertIs<Message>(asyncApiContext.findReference(reference))
        assertEquals("RootEvent", message.name)
        assertEquals("message-root.yaml", asyncApiContext.getSourceLocation(message)?.file?.name)
    }

    @Test
    fun `loads a whole file containing one server object`() {
        parseDocument("parser/references/external/server-root-main.yaml")

        val reference = assertIs<Reference>(
            asyncApiContext.modelRepository.getModelsByPath()[
                "server_root_main.root.components.servers.RootServer"
            ],
        )
        val server = assertIs<Server>(asyncApiContext.findReference(reference))
        assertEquals("events.example.com", server.host)
        assertEquals("server-root.yaml", asyncApiContext.getSourceLocation(server)?.file?.name)
    }

    @Test
    fun `rejects a whole file message container without an explicit pointer`() {
        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parseDocument("parser/references/external/message-map-main.yaml")
        }

        val diagnostic = assertIs<ParserDiagnostic.UnexpectedObjectMember>(error.diagnostic)
        assertEquals(ParserDiagnosticCategory.UNEXPECTED_OBJECT_MEMBER, diagnostic.category)
        assertEquals("ExternalEvent", diagnostic.memberName)
        assertEquals("messages.root.ExternalEvent", diagnostic.path)
        assertEquals("messages.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `reports malformed external reference at the referencing field`() {
        val diagnostic = assertReferenceFailure<ParserDiagnostic.InvalidReference>("MalformedReference")

        assertEquals(ParserDiagnosticCategory.INVALID_REFERENCE, diagnostic.category)
        assertEquals("./fragments.yaml#not-a-pointer", diagnostic.reference)
        assertEquals(
            "external_reference_invalid.root.components.schemas.MalformedReference.\$ref",
            diagnostic.path,
        )
        assertEquals("external_reference_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `reports missing external document at the referencing field`() {
        val diagnostic =
            assertReferenceFailure<ParserDiagnostic.ReferenceDocumentNotFound>("MissingDocument")

        assertEquals(ParserDiagnosticCategory.REFERENCE_DOCUMENT_NOT_FOUND, diagnostic.category)
        assertEquals("./does-not-exist.yaml#/Missing", diagnostic.reference)
        assertEquals(
            "external_reference_invalid.root.components.schemas.MissingDocument.\$ref",
            diagnostic.path,
        )
        assertEquals("external_reference_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `reports missing external pointer target at the referencing field`() {
        val diagnostic =
            assertReferenceFailure<ParserDiagnostic.ReferenceTargetNotFound>("MissingTarget")

        assertEquals(ParserDiagnosticCategory.REFERENCE_TARGET_NOT_FOUND, diagnostic.category)
        assertEquals("./fragments.yaml#/components/schemas/Absent", diagnostic.reference)
        assertEquals(
            "external_reference_invalid.root.components.schemas.MissingTarget.\$ref",
            diagnostic.path,
        )
        assertEquals("external_reference_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    private fun parseValidReferences(): Map<String, SchemaInterface> =
        parser.parseMap(
            readNode(
                "parser/references/external/main.yaml",
                "components",
                "schemas",
            ),
        )

    private fun resolvedSchema(
        schemas: Map<String, SchemaInterface>,
        name: String,
    ): Schema {
        val reference = (schemas.getValue(name) as SchemaInterface.SchemaReference).reference
        return assertIs<Schema>(asyncApiContext.findReference(reference))
    }

    private fun resolvedBooleanSchema(
        schemas: Map<String, SchemaInterface>,
        name: String,
    ): SchemaInterface.BooleanSchema {
        val reference = (schemas.getValue(name) as SchemaInterface.SchemaReference).reference
        return assertIs<SchemaInterface.BooleanSchema>(asyncApiContext.findReference(reference))
    }

    private inline fun <reified T : ParserDiagnostic> assertReferenceFailure(
        name: String,
    ): T {
        val node = readNode(
            "parser/references/external/external_reference_invalid.yaml",
            "components",
            "schemas",
            name,
        )
        val exception = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(node)
        }
        return assertIs<T>(exception.diagnostic)
    }
}
