package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.parser.schemas.SchemaParser
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiParserProfile
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class ExternalReferenceLoadingTest {
    private val context = AsyncApiContext()
    private val schemaParser = SchemaParser(context)
    private val documentParser = AsyncApiParser(context)

    @Test
    fun `loads escaped pointers root schemas and equivalent yaml and json fragments`() {
        val file = TestResources.file("parser/references/external/main.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemasNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
        val schemas = schemaParser.parseMap(schemasNode)

        val escapedReference = assertIs<SchemaInterface.SchemaReference>(
            schemas.getValue("EscapedReference"),
        ).reference
        val escapedSchema = assertIs<Schema>(context.findReference(escapedReference))
        assertEquals("escaped pointer target", escapedSchema.description)

        val rootReference = assertIs<SchemaInterface.SchemaReference>(
            schemas.getValue("RootReference"),
        ).reference
        val rootSchema = assertIs<Schema>(context.findReference(rootReference))
        assertEquals("root schema target", rootSchema.description)

        val yamlReference = assertIs<SchemaInterface.SchemaReference>(
            schemas.getValue("YamlReference"),
        ).reference
        val jsonReference = assertIs<SchemaInterface.SchemaReference>(
            schemas.getValue("JsonReference"),
        ).reference
        val yamlSchema = assertIs<Schema>(context.findReference(yamlReference))
        val jsonSchema = assertIs<Schema>(context.findReference(jsonReference))
        assertEquals(yamlSchema.description, jsonSchema.description)

        val encodedReference = assertIs<SchemaInterface.SchemaReference>(
            schemas.getValue("EncodedReference"),
        ).reference
        val encodedSchema = assertIs<Schema>(context.findReference(encodedReference))
        assertEquals("percent encoded URI and pointer", encodedSchema.description)

        val booleanReference = assertIs<SchemaInterface.SchemaReference>(
            schemas.getValue("BooleanRootReference"),
        ).reference
        val booleanSchema = assertIs<SchemaInterface.BooleanSchema>(context.findReference(booleanReference))
        assertEquals(true, booleanSchema.value)

        val arrayReference = assertIs<SchemaInterface.SchemaReference>(
            schemas.getValue("ArrayRootReference"),
        ).reference
        val arraySchema = assertIs<Schema>(context.findReference(arrayReference))
        assertEquals("array root target", arraySchema.description)
    }

    @Test
    fun `keeps same named external documents in different directories distinct`() {
        val file = TestResources.file("parser/references/external/main.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemasNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
        val schemas = schemaParser.parseMap(schemasNode)

        val leftReference = assertIs<SchemaInterface.SchemaReference>(
            schemas.getValue("LeftReference"),
        ).reference
        val rightReference = assertIs<SchemaInterface.SchemaReference>(
            schemas.getValue("RightReference"),
        ).reference
        val left = assertIs<Schema>(context.findReference(leftReference))
        val right = assertIs<Schema>(context.findReference(rightReference))
        assertEquals("left shared schema", left.description)
        assertEquals("right shared schema", right.description)

        val leftLocation = assertNotNull(context.getSourceLocation(left))
        val rightLocation = assertNotNull(context.getSourceLocation(right))
        assertEquals("shared.yaml", leftLocation.file.name)
        assertEquals("shared.yaml", rightLocation.file.name)
        assertNotEquals(leftLocation.file.canonicalPath, rightLocation.file.canonicalPath)
        assertNotEquals(
            context.sourceRepository.findIdByFile(leftLocation.file),
            context.sourceRepository.findIdByFile(rightLocation.file),
        )
    }

    @Test
    fun `dispatches a standalone external fragment using its reference category`() {
        val file = TestResources.file("parser/references/external/message-main.yaml")
        val document = DocumentReaderRegistry.read(file)
        val root = ParserNodeFactory.root(document, context)
        documentParser.parse(root)

        val reference = assertIs<Reference>(
            context.modelRepository.getModelsByPath()[
                "message_main.root.components.messages.ExternalEvent"
            ],
        )
        assertEquals(
            AsyncApiParserProfile.V3_0,
            context.modelRepository.getReferenceOrigin(reference)?.parserProfile,
        )
        val message = assertNotNull(
            context.modelRepository.getModelsByPath()["messages.root.ExternalEvent"],
        )
        assertEquals("messages.yaml", context.getSourceLocation(message)?.file?.name)
    }

    @Test
    fun `loads a whole file containing one message object`() {
        val file = TestResources.file("parser/references/external/message-root-main.yaml")
        val document = DocumentReaderRegistry.read(file)
        val root = ParserNodeFactory.root(document, context)
        documentParser.parse(root)

        val reference = assertIs<Reference>(
            context.modelRepository.getModelsByPath()[
                "message_root_main.root.components.messages.RootEvent"
            ],
        )
        val message = assertIs<Message>(context.findReference(reference))
        assertEquals("RootEvent", message.name)
        assertEquals("message-root.yaml", context.getSourceLocation(message)?.file?.name)
    }

    @Test
    fun `loads a whole file containing one server object`() {
        val file = TestResources.file("parser/references/external/server-root-main.yaml")
        val document = DocumentReaderRegistry.read(file)
        val root = ParserNodeFactory.root(document, context)
        documentParser.parse(root)

        val reference = assertIs<Reference>(
            context.modelRepository.getModelsByPath()[
                "server_root_main.root.components.servers.RootServer"
            ],
        )
        val server = assertIs<Server>(context.findReference(reference))
        assertEquals("events.example.com", server.host)
        assertEquals("server-root.yaml", context.getSourceLocation(server)?.file?.name)
    }

    @Test
    fun `rejects a whole file message container without an explicit pointer`() {
        val file = TestResources.file("parser/references/external/message-map-main.yaml")
        val document = DocumentReaderRegistry.read(file)
        val root = ParserNodeFactory.root(document, context)

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            documentParser.parse(root)
        }

        val diagnostic = assertIs<ParserDiagnostic.UnexpectedObjectMember>(error.diagnostic)
        assertEquals(ParserDiagnosticCategory.UNEXPECTED_OBJECT_MEMBER, diagnostic.category)
        assertEquals("ExternalEvent", diagnostic.memberName)
        assertEquals("messages.root.ExternalEvent", diagnostic.path)
        assertEquals("messages.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `reports malformed external reference at the referencing field`() {
        val file = TestResources.file("parser/references/external/external_reference_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("MalformedReference")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            schemaParser.parseElement(schemaNode)
        }

        val diagnostic = assertIs<ParserDiagnostic.InvalidReference>(error.diagnostic)
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
        val file = TestResources.file("parser/references/external/external_reference_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("MissingDocument")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            schemaParser.parseElement(schemaNode)
        }

        val diagnostic = assertIs<ParserDiagnostic.ReferenceDocumentNotFound>(error.diagnostic)
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
        val file = TestResources.file("parser/references/external/external_reference_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("MissingTarget")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            schemaParser.parseElement(schemaNode)
        }

        val diagnostic = assertIs<ParserDiagnostic.ReferenceTargetNotFound>(error.diagnostic)
        assertEquals(ParserDiagnosticCategory.REFERENCE_TARGET_NOT_FOUND, diagnostic.category)
        assertEquals("./fragments.yaml#/components/schemas/Absent", diagnostic.reference)
        assertEquals(
            "external_reference_invalid.root.components.schemas.MissingTarget.\$ref",
            diagnostic.path,
        )
        assertEquals("external_reference_invalid.yaml", diagnostic.sourceLocation.file.name)
    }
}
