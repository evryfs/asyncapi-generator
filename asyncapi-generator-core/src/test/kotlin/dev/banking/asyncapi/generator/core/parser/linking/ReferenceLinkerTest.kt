package dev.banking.asyncapi.generator.core.parser.linking

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
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
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class ReferenceLinkerTest {
    @Test
    fun `links every reference and reports where each chain leads`() {
        val context = AsyncApiContext()
        val source =
            DocumentSource(
                file = File("linking.yaml").canonicalFile,
                content =
                    """
                    asyncapi: 3.0.0
                    info:
                      title: Linking
                      version: 1.0.0
                    components:
                      schemas:
                        Target:
                          type: string
                        Direct:
                          ${'$'}ref: '#/components/schemas/Target'
                        Chained:
                          ${'$'}ref: '#/components/schemas/Direct'
                        Missing:
                          ${'$'}ref: '#/components/schemas/Absent'
                        Loop:
                          ${'$'}ref: '#/components/schemas/Loop'
                    """.trimIndent(),
                format = InputFormat.YAML,
            )
        val document = AsyncApiParser(context).parse(ParserNodeFactory.root(SourceReader.read(source), context))
        val schemas = assertNotNull(assertNotNull(document.components).schemas)

        fun reference(name: String) = assertIs<SchemaInterface.SchemaReference>(schemas.getValue(name)).reference

        context.referenceLinker.linkAll(context.parsedReferences.since(0).map { it.reference })

        val target = assertIs<SchemaInterface.SchemaInline>(schemas.getValue("Target")).schema
        val chained = assertIs<ReferenceLink.Resolved>(context.referenceLinker.link(reference("Chained")))
        assertSame(target, chained.target)
        assertEquals(listOf(reference("Chained"), reference("Direct")), chained.chain)
        assertSame(reference("Direct"), reference("Chained").model)
        assertSame(target, reference("Chained").requireModel<Schema>())
        assertIs<ReferenceLink.Unresolved>(context.referenceLinker.link(reference("Missing")))
        assertIs<ReferenceLink.Cycle>(context.referenceLinker.link(reference("Loop")))
    }
}
