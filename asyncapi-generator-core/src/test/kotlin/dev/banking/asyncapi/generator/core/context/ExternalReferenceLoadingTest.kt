package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.CHANNEL
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationId
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserLoadResourceLimit
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddress
import dev.banking.asyncapi.generator.core.model.operations.OperationTrait
import dev.banking.asyncapi.generator.core.model.parameters.Parameter
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.security.SecurityScheme
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.servers.ServerVariable
import dev.banking.asyncapi.generator.core.model.tags.Tag
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.parser.schemas.SchemaParser
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiParserProfile
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class ExternalReferenceLoadingTest {
    @TempDir
    lateinit var tempDir: Path

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

        fun resolvedType(referenceName: String): String? {
            val reference = assertIs<SchemaInterface.SchemaReference>(schemas.getValue(referenceName)).reference
            return assertIs<Schema>(context.findReference(reference)).type as? String
        }

        assertEquals("integer", resolvedType("DottedExternalReference"))
        assertEquals("string", resolvedType("NestedExternalReference"))
        assertEquals("number", resolvedType("BracketExternalReference"))
        assertEquals("boolean", resolvedType("NumericMemberExternalReference"))
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
    fun `parses and validates every concrete external fragment category`() {
        val file = TestResources.file("parser/references/external/category-main.yaml")
        val document = DocumentReaderRegistry.read(file)
        val root = ParserNodeFactory.root(document, context)

        documentParser.parse(root)

        val models = context.modelRepository.getModelsByPath()
        assertIs<Schema>(models["category_fragments.root.schema"])
        assertIs<Channel>(models["category_fragments.root.channel"])
        assertIs<Message>(models["category_fragments.root.message"])
        assertIs<MessageTrait>(models["category_fragments.root.messageTrait"])
        assertIs<Operation>(models["category_fragments.root.operation"])
        assertIs<OperationTrait>(models["category_fragments.root.operationTrait"])
        assertIs<OperationReply>(models["category_fragments.root.operationReply"])
        assertIs<OperationReplyAddress>(models["category_fragments.root.operationReplyAddress"])
        assertIs<Server>(models["category_fragments.root.server"])
        assertIs<ServerVariable>(models["category_fragments.root.serverVariable"])
        assertIs<Parameter>(models["category_fragments.root.parameter"])
        assertIs<SecurityScheme>(models["category_fragments.root.securityScheme"])
        assertIs<CorrelationId>(models["category_fragments.root.correlationId"])
        assertIs<ExternalDoc>(models["category_fragments.root.externalDoc"])
        assertIs<Tag>(models["category_fragments.root.tag"])
        val binding = assertIs<Binding>(models["category_fragments.root.binding"])
        assertEquals("category-fragments.yaml", context.getSourceLocation(binding)?.file?.name)
    }

    @Test
    fun `retains protocol and location when loading an external binding fragment`() {
        val file = TestResources.file("parser/references/external/binding-main.yaml")
        val document = DocumentReaderRegistry.read(file)
        val root = ParserNodeFactory.root(document, context)

        documentParser.parse(root)

        val binding = assertIs<Binding>(
            context.modelRepository.getModelsByPath()["binding_fragments.root.channelKafka"],
        )
        val protocolBinding = binding.protocolBindings.single()
        assertEquals("kafka", protocolBinding.protocol)
        assertEquals(CHANNEL, protocolBinding.location)
        assertEquals("0.4.0", protocolBinding.bindingVersion)
        assertEquals("binding-fragments.yaml", context.getSourceLocation(protocolBinding)?.file?.name)
    }

    @Test
    fun `isolates selected fragments and resolves same-file reference chains and cycles`() {
        val file = TestResources.file("parser/references/external/external-fragment-isolation-main.yaml")
        val document = DocumentReaderRegistry.read(file)
        val root = ParserNodeFactory.root(document, context)

        documentParser.parse(root)

        val models = context.modelRepository.getModelsByPath()
        val selected = assertIs<Schema>(models["heterogeneous_fragments.root.selectedSchema"])
        assertEquals("Only this selected schema should be parsed", selected.description)
        assertFalse(models.containsKey("heterogeneous_fragments.root.notASchema"))

        val first = assertIs<Reference>(models["heterogeneous_fragments.root.firstSchema"])
        val second = assertIs<Reference>(context.findReference(first))
        val third = assertIs<Schema>(context.findReference(second))
        assertEquals("End of the same-file reference chain", third.description)
        assertEquals("heterogeneous-fragments.yaml", context.getSourceLocation(third)?.file?.name)
        assertEquals("heterogeneous_fragments.root.thirdSchema", context.getSourceLocation(third)?.path)

        val cycleA = assertIs<Reference>(models["heterogeneous_fragments.root.cycleA"])
        val cycleB = assertIs<Reference>(context.findReference(cycleA))
        assertSame(cycleA, context.findReference(cycleB))
        assertSame(cycleB, cycleA.model)
        assertSame(cycleA, cycleB.model)
    }

    @Test
    fun `reports a missing same-file fragment target at the nested reference`() {
        val file = TestResources.file("parser/references/external/external-fragment-internal-missing-main.yaml")
        val document = DocumentReaderRegistry.read(file)
        val root = ParserNodeFactory.root(document, context)

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            documentParser.parse(root)
        }

        val diagnostic = assertIs<ParserDiagnostic.ReferenceTargetNotFound>(error.diagnostic)
        assertEquals(ParserDiagnosticCategory.REFERENCE_TARGET_NOT_FOUND, diagnostic.category)
        assertEquals("#/doesNotExist", diagnostic.reference)
        assertEquals("heterogeneous_fragments.root.missingReference.\$ref", diagnostic.path)
        assertEquals("heterogeneous-fragments.yaml", diagnostic.sourceLocation.file.name)
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

    @Test
    fun `limits distinct source documents at the reference that exceeds the budget`() {
        val fragment = tempDir.resolve("fragment.yaml").toFile()
        fragment.writeText("type: string")
        val main = tempDir.resolve("main.yaml").toFile()
        main.writeText(
            """
            asyncapi: 3.0.0
            info:
              title: Document limit
              version: 1.0.0
            components:
              schemas:
                External:
                  ${'$'}ref: ./fragment.yaml
            """.trimIndent(),
        )
        val limitedContext = AsyncApiContext(
            ParserLoadResourceLimits(maxSourceDocuments = 1),
        )
        val root = ParserNodeFactory.root(DocumentReaderRegistry.read(main), limitedContext)

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            AsyncApiParser(limitedContext).parse(root)
        }

        val diagnostic = assertIs<ParserDiagnostic.LoadResourceLimitExceeded>(error.diagnostic)
        assertEquals(ParserDiagnosticCategory.LOAD_RESOURCE_LIMIT_EXCEEDED, diagnostic.category)
        assertEquals(ParserLoadResourceLimit.SOURCE_DOCUMENTS, diagnostic.limit)
        assertEquals(1L, diagnostic.maximum)
        assertEquals(2L, diagnostic.observed)
        assertEquals("main.root.components.schemas.External.\$ref", diagnostic.path)
        assertEquals(main.canonicalFile, diagnostic.sourceLocation.file.canonicalFile)
    }

    @Test
    fun `limits unique resolved targets while counting repeated targets once`() {
        val nestedDirectory = tempDir.resolve("nested").toFile()
        nestedDirectory.mkdirs()
        val fragment = tempDir.resolve("fragment.yaml").toFile()
        fragment.writeText(
            """
            selected:
              type: string
            other:
              type: integer
            """.trimIndent(),
        )
        val repeatedMain = tempDir.resolve("repeated-main.yaml").toFile()
        repeatedMain.writeText(
            """
            asyncapi: 3.0.0
            info:
              title: Repeated target
              version: 1.0.0
            components:
              schemas:
                Direct:
                  ${'$'}ref: ./fragment.yaml#/selected
                CanonicalAlias:
                  ${'$'}ref: ./nested/../fragment.yaml#/selected
            """.trimIndent(),
        )
        val repeatedContext = AsyncApiContext(
            ParserLoadResourceLimits(
                maxSourceDocuments = 2,
                maxReferenceTargets = 1,
            ),
        )
        val repeatedRoot = ParserNodeFactory.root(
            DocumentReaderRegistry.read(repeatedMain),
            repeatedContext,
        )

        AsyncApiParser(repeatedContext).parse(repeatedRoot)

        val distinctMain = tempDir.resolve("distinct-main.yaml").toFile()
        distinctMain.writeText(
            repeatedMain.readText().replace(
                "./nested/../fragment.yaml#/selected",
                "./fragment.yaml#/other",
            ),
        )
        val distinctContext = AsyncApiContext(
            ParserLoadResourceLimits(maxReferenceTargets = 1),
        )
        val distinctRoot = ParserNodeFactory.root(
            DocumentReaderRegistry.read(distinctMain),
            distinctContext,
        )

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            AsyncApiParser(distinctContext).parse(distinctRoot)
        }

        val diagnostic = assertIs<ParserDiagnostic.LoadResourceLimitExceeded>(error.diagnostic)
        assertEquals(ParserLoadResourceLimit.REFERENCE_TARGETS, diagnostic.limit)
        assertEquals(1L, diagnostic.maximum)
        assertEquals(2L, diagnostic.observed)
        assertEquals("distinct_main.root.components.schemas.CanonicalAlias.\$ref", diagnostic.path)
    }

    @Test
    fun `limits acyclic external reference depth without charging cycles repeatedly`() {
        val first = tempDir.resolve("first.yaml").toFile()
        val second = tempDir.resolve("second.yaml").toFile()
        val third = tempDir.resolve("third.yaml").toFile()
        first.writeText("${'$'}ref: ./second.yaml")
        second.writeText("${'$'}ref: ./third.yaml")
        third.writeText("type: string")
        val main = tempDir.resolve("depth-main.yaml").toFile()
        main.writeText(
            """
            asyncapi: 3.0.0
            info:
              title: Depth limit
              version: 1.0.0
            components:
              schemas:
                External:
                  ${'$'}ref: ./first.yaml
            """.trimIndent(),
        )
        val limitedContext = AsyncApiContext(
            ParserLoadResourceLimits(maxExternalReferenceDepth = 2),
        )
        val limitedRoot = ParserNodeFactory.root(DocumentReaderRegistry.read(main), limitedContext)

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            AsyncApiParser(limitedContext).parse(limitedRoot)
        }

        val diagnostic = assertIs<ParserDiagnostic.LoadResourceLimitExceeded>(error.diagnostic)
        assertEquals(ParserLoadResourceLimit.EXTERNAL_REFERENCE_DEPTH, diagnostic.limit)
        assertEquals(2L, diagnostic.maximum)
        assertEquals(3L, diagnostic.observed)
        assertEquals("second.root.\$ref", diagnostic.path)
        assertEquals(second.canonicalFile, diagnostic.sourceLocation.file.canonicalFile)

        first.writeText("${'$'}ref: ./second.yaml")
        second.writeText("${'$'}ref: ./first.yaml")
        val cyclicContext = AsyncApiContext(
            ParserLoadResourceLimits(
                maxReferenceTargets = 2,
                maxExternalReferenceDepth = 2,
            ),
        )
        val cyclicRoot = ParserNodeFactory.root(DocumentReaderRegistry.read(main), cyclicContext)

        AsyncApiParser(cyclicContext).parse(cyclicRoot)
    }

    @Test
    fun `limits aggregate canonical source bytes at the causing reference`() {
        val fragment = tempDir.resolve("aggregate-fragment.yaml").toFile()
        fragment.writeText("type: string\ndescription: aggregate bytes")
        val main = tempDir.resolve("aggregate-main.yaml").toFile()
        main.writeText(
            """
            asyncapi: 3.0.0
            info:
              title: Aggregate limit
              version: 1.0.0
            components:
              schemas:
                External:
                  ${'$'}ref: ./aggregate-fragment.yaml
            """.trimIndent(),
        )
        val mainBytes = main.readBytes().size.toLong()
        val fragmentBytes = fragment.readBytes().size.toLong()
        val limitedContext = AsyncApiContext(
            ParserLoadResourceLimits(
                maxAggregateSourceBytes = mainBytes + fragmentBytes - 1,
            ),
        )
        val root = ParserNodeFactory.root(DocumentReaderRegistry.read(main), limitedContext)

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            AsyncApiParser(limitedContext).parse(root)
        }

        val diagnostic = assertIs<ParserDiagnostic.LoadResourceLimitExceeded>(error.diagnostic)
        assertEquals(ParserLoadResourceLimit.AGGREGATE_SOURCE_BYTES, diagnostic.limit)
        assertEquals(mainBytes + fragmentBytes - 1, diagnostic.maximum)
        assertEquals(mainBytes + fragmentBytes, diagnostic.observed)
        assertEquals("aggregate_main.root.components.schemas.External.\$ref", diagnostic.path)
    }
}
