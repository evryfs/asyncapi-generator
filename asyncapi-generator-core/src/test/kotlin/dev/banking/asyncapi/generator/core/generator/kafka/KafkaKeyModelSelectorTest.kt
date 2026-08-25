package dev.banking.asyncapi.generator.core.generator.kafka

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMultiFormatMessage
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.schema.SchemaDeclarationCatalog
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class KafkaKeyModelSelectorTest {
    @Test
    fun `selects native payload object key models and their dependencies`() {
        val tenantSchema =
            Schema(
                type = "object",
                properties =
                    mapOf(
                        "tenantId" to SchemaInterface.SchemaInline(Schema(type = "string")),
                    ),
            )
        val keySchema =
            Schema(
                title = "AccountKey",
                type = "object",
                properties =
                    mapOf(
                        "tenant" to
                            SchemaInterface.SchemaReference(
                                Reference(
                                    ref = "#/components/schemas/Tenant",
                                    model = tenantSchema,
                                ),
                            ),
                    ),
            )
        val input =
            GenerationInput(
                schemas =
                    linkedMapOf(
                        "AccountKey" to keySchema,
                        "Tenant" to tenantSchema,
                        "Unrelated" to Schema(type = "object"),
                    ),
                schemaDeclarations =
                    SchemaDeclarationCatalog(
                        multiFormatSchemas = mapOf("AccountUpdated" to nativeAvroSchema()),
                    ),
                polymorphicRelationships = emptyMap(),
                channels =
                    listOf(
                        AnalyzedChannel(
                            channelName = "accountEvents",
                            topic = "accounts.events",
                            messages = emptyList(),
                            multiFormatMessages =
                                listOf(
                                    AnalyzedMultiFormatMessage(
                                        messageName = "AccountUpdated",
                                        payloadName = "AccountUpdated",
                                        schema = nativeAvroSchema(),
                                        keySchema = SchemaInterface.SchemaInline(keySchema),
                                    ),
                                ),
                        ),
                    ),
            )

        val selected = KafkaKeyModelSelector.select(input)

        assertEquals(listOf("AccountKey", "Tenant"), selected.keys.toList())
    }

    @Test
    fun `does not select primitive Kafka keys`() {
        val input =
            GenerationInput(
                schemas = emptyMap(),
                polymorphicRelationships = emptyMap(),
                channels =
                    listOf(
                        AnalyzedChannel(
                            channelName = "accountEvents",
                            topic = "accounts.events",
                            messages = emptyList(),
                            multiFormatMessages =
                                listOf(
                                    AnalyzedMultiFormatMessage(
                                        messageName = "AccountUpdated",
                                        payloadName = "AccountUpdated",
                                        schema = nativeAvroSchema(),
                                        keySchema = SchemaInterface.SchemaInline(Schema(type = "string")),
                                    ),
                                ),
                        ),
                    ),
            )

        assertEquals(emptyMap(), KafkaKeyModelSelector.select(input))
    }

    private fun nativeAvroSchema(): MultiFormatSchema =
        MultiFormatSchema(
            schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
            schema = mapOf("type" to "record", "name" to "AccountUpdated", "fields" to emptyList<Any>()),
        )
}
