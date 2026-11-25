package dev.banking.asyncapi.generator.core.parser.tracking

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.modelAt
import dev.banking.asyncapi.generator.core.model.schemas.JsonSchemaType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.source.DocumentSource
import dev.banking.asyncapi.generator.core.source.InputFormat
import dev.banking.asyncapi.generator.core.source.SourceReader
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class ModelTrackingTest {
    @Test
    fun `references resolve through typed addresses for adversarial member names`() {
        val context = AsyncApiContext()
        val source =
            DocumentSource(
                file = File("identity.yaml").canonicalFile,
                content =
                    """
                    asyncapi: 3.0.0
                    info:
                      title: Collision-safe identity
                      version: 1.0.0
                    components:
                      schemas:
                        A:
                          type: object
                          properties:
                            x:
                              type: string
                        "A.properties.x":
                          type: integer
                        "slash/name~":
                          type: boolean
                        "bracket[name]":
                          type: number
                        "0":
                          type: string
                        Tuple:
                          type: array
                          items:
                            - type: string
                            - type: number
                        DottedReference:
                          ${'$'}ref: '#/components/schemas/A.properties.x'
                        NestedReference:
                          ${'$'}ref: '#/components/schemas/A/properties/x'
                        EscapedReference:
                          ${'$'}ref: '#/components/schemas/slash~1name~0'
                        BracketReference:
                          ${'$'}ref: '#/components/schemas/bracket%5Bname%5D'
                        NumericMemberReference:
                          ${'$'}ref: '#/components/schemas/0'
                        NumericIndexReference:
                          ${'$'}ref: '#/components/schemas/Tuple/items/0'
                    """.trimIndent(),
                format = InputFormat.YAML,
            )
        val root = ParserNodeFactory.root(SourceReader.read(source), context)
        val document = AsyncApiParser(context).parse(root)
        val components = assertNotNull(document.components)
        val schemas = assertNotNull(components.schemas)

        fun resolvedSchema(referenceName: String): Schema {
            val reference = assertIs<SchemaInterface.SchemaReference>(schemas.getValue(referenceName)).reference
            return assertIs<Schema>(context.modelTracking.findReference(reference))
        }

        val dotted = resolvedSchema("DottedReference")
        val nested = resolvedSchema("NestedReference")
        assertEquals(JsonSchemaType.Single("integer"), dotted.type)
        assertEquals(JsonSchemaType.Single("string"), nested.type)
        assertEquals(JsonSchemaType.Single("boolean"), resolvedSchema("EscapedReference").type)
        assertEquals(JsonSchemaType.Single("number"), resolvedSchema("BracketReference").type)
        assertEquals(JsonSchemaType.Single("string"), resolvedSchema("NumericMemberReference").type)
        assertEquals(JsonSchemaType.Single("string"), resolvedSchema("NumericIndexReference").type)
        assertEquals(
            "identity.root.components.schemas[\"A.properties.x\"]",
            context.modelTracking.getSourceLocation(dotted)?.path,
        )
        assertEquals(
            "identity.root.components.schemas.A.properties.x",
            context.modelTracking.getSourceLocation(nested)?.path,
        )

        val nestedPath = "identity.root.components.schemas.A.properties.x"
        val dottedPath = "identity.root.components.schemas[\"A.properties.x\"]"
        assertNotEquals(context.modelAt(nestedPath), context.modelAt(dottedPath))
        assertIs<Schema>(context.modelAt("identity.root.components.schemas[\"bracket[name]\"]"))
        assertIs<Schema>(context.modelAt("identity.root.components.schemas.0"))
        assertIs<Schema>(context.modelAt("identity.root.components.schemas.Tuple.items[0]"))
    }
}
