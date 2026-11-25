package dev.banking.asyncapi.generator.core.generator.sourcemodel

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.fixtures.generationInputOf
import dev.banking.asyncapi.generator.core.generator.jvm.JvmNaming
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.JsonSchemaType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ModelDefinitionFactoryTest {
    @Test
    fun `creates object and enum models and skips scalar aliases`() {
        val models =
            ModelDefinitionFactory(
                input = GenerationInputFixtures().generationInputWithObjectEnumAndPrimitive(),
                packageName = "com.example.model",
                naming = JvmNaming,
            ).create()

        assertEquals(listOf("User", "Status"), models.map { it.name })
        val user = assertIs<ObjectModel>(models[0])
        assertEquals(listOf("Command"), user.unions)
        assertEquals(TypeRef.Scalar.STRING, user.properties.single().type)
        assertEquals(true, user.properties.single().required)
        assertEquals(listOf("ACTIVE", "INACTIVE"), assertIs<EnumModel>(models[1]).constants.map { it.name })
    }

    @Test
    fun `creates union and open object models`() {
        val models =
            create(
                "Command" to Schema(oneOf = listOf(ref("Start")), discriminator = "kind"),
                "Start" to
                    Schema(
                        type = JsonSchemaType.Single("object"),
                        properties =
                            mapOf("kind" to inline(Schema(type = JsonSchemaType.Single("string"), const = "start"))),
                    ),
                "Anything" to Schema(type = JsonSchemaType.Single("object")),
            )

        val union = assertIs<UnionModel>(models[0])
        assertEquals("kind", union.discriminator)
        assertEquals(listOf(UnionSubtype(name = "start", type = "Start")), union.subtypes)
        assertIs<OpenObjectModel>(models[2])
    }

    @Test
    fun `resolves property types, identifiers, and defaults`() {
        val models =
            create(
                "Order" to
                    Schema(
                        type = JsonSchemaType.Single("object"),
                        required = listOf("status"),
                        properties =
                            mapOf(
                                "status" to SchemaInterface.SchemaReference(Reference("#/components/schemas/Status")),
                                "order-name" to SchemaInterface.SchemaReference(Reference("#/components/schemas/Name")),
                                "count" to inline(Schema(type = JsonSchemaType.Single("integer"), default = 3)),
                            ),
                    ),
                "Status" to
                    Schema(type = JsonSchemaType.Single("string"), enum = listOf("open", "closed"), default = "open"),
                "Name" to Schema(type = JsonSchemaType.Multiple(listOf("string", "null")), maxLength = 20),
            )

        val (status, name, count) = assertIs<ObjectModel>(models.first()).properties
        assertEquals(TypeRef.Named("Status", isModel = false), status.type)
        assertEquals(DefaultValue.EnumEntry("Status", "OPEN"), status.default)
        assertEquals("orderName", name.identifier)
        assertEquals("order-name", name.wireName)
        assertEquals(TypeRef.Scalar.STRING, name.type)
        assertEquals(true, name.nullable)
        assertEquals(DefaultValue.IntegerLiteral("3"), count.default)
    }

    private fun create(vararg schemas: Pair<String, Schema>): List<ModelDefinition> =
        ModelDefinitionFactory(
            input =
                generationInputOf(
                    schemas = linkedMapOf(*schemas),
                    polymorphicRelationships = emptyMap(),
                    channels = emptyList(),
                ),
            packageName = "com.example.model",
            naming = JvmNaming,
        ).create()

    private fun inline(schema: Schema) = SchemaInterface.SchemaInline(schema)

    private fun ref(name: String) = SchemaInterface.SchemaReference(Reference("#/components/schemas/$name"))

    @Test
    fun `fails when two properties become the same identifier`() {
        val failure =
            kotlin.test.assertFailsWith<AsyncApiGeneratorException.PropertyNameCollision> {
                create(
                    "Account" to
                        Schema(
                            type = JsonSchemaType.Single("object"),
                            properties =
                                mapOf(
                                    "account-id" to inline(Schema(type = JsonSchemaType.Single("string"))),
                                    "accountId" to inline(Schema(type = JsonSchemaType.Single("string"))),
                                ),
                        ),
                )
            }

        assertEquals(listOf("account-id", "accountId"), failure.wireNames)
        kotlin.test.assertTrue("Account" in failure.message.orEmpty())
    }

    @Test
    fun `parses string defaults of date and uuid properties`() {
        val order =
            assertIs<ObjectModel>(
                create(
                    "Order" to
                        Schema(
                            type = JsonSchemaType.Single("object"),
                            properties =
                                mapOf(
                                    "day" to
                                        inline(
                                            Schema(
                                                type = JsonSchemaType.Single("string"),
                                                format = "date",
                                                default = "2024-01-01",
                                            ),
                                        ),
                                    "note" to inline(Schema(type = JsonSchemaType.Single("string"), default = "n")),
                                ),
                        ),
                ).single(),
            )

        assertEquals(DefaultValue.ParsedScalar(TypeRef.Scalar.DATE, "2024-01-01"), order.properties[0].default)
        assertEquals(DefaultValue.StringLiteral("n"), order.properties[1].default)
    }
}
