package dev.banking.asyncapi.generator.core.generator.ir

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.model.UnionSubtype
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ModelDefinitionFactoryTest {
    @Test
    fun `creates object and enum models and skips scalar aliases`() {
        val annotation = QualifiedTypeName.fromConfigurationValue("jakarta.validation.Valid", "modelConfig.modelAnnotation")

        val models =
            ModelDefinitionFactory(
                input = GenerationInputFixtures().generationInputWithObjectEnumAndPrimitive(),
                packageName = "com.example.model",
                annotation = annotation,
            ).create()

        assertEquals(listOf("User", "Status"), models.map { it.name })
        val user = assertIs<ObjectModel>(models[0])
        assertEquals(listOf("Command"), user.unions)
        assertEquals(annotation, user.annotation)
        assertEquals(TypeRef.Scalar.STRING, user.properties.single().type)
        assertEquals(true, user.properties.single().required)
        assertEquals(listOf("ACTIVE", "INACTIVE"), assertIs<EnumModel>(models[1]).constants.map { it.name })
    }

    @Test
    fun `creates union and open object models`() {
        val models =
            create(
                "Command" to Schema(oneOf = listOf(ref("Start")), discriminator = "kind"),
                "Start" to Schema(type = "object", properties = mapOf("kind" to inline(Schema(type = "string", const = "start")))),
                "Anything" to Schema(type = "object"),
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
                        type = "object",
                        required = listOf("status"),
                        properties =
                            mapOf(
                                "status" to SchemaInterface.SchemaReference(Reference("#/components/schemas/Status")),
                                "order-name" to SchemaInterface.SchemaReference(Reference("#/components/schemas/Name")),
                                "count" to inline(Schema(type = "integer", default = 3)),
                            ),
                    ),
                "Status" to Schema(type = "string", enum = listOf("open", "closed"), default = "open"),
                "Name" to Schema(type = listOf("string", "null"), maxLength = 20),
            )

        val (status, name, count) = assertIs<ObjectModel>(models.first()).properties
        assertEquals(TypeRef.Named("Status", isModel = false), status.type)
        assertEquals(DefaultValue.EnumEntry("Status", "OPEN"), status.default)
        assertEquals("orderName", name.identifier)
        assertEquals("order-name", name.wireName)
        assertEquals(TypeRef.Scalar.STRING, name.type)
        assertEquals(true, name.nullableInSchema)
        assertEquals(DefaultValue.IntegerLiteral("3"), count.default)
    }

    private fun create(vararg schemas: Pair<String, Schema>): List<ModelDefinition> =
        ModelDefinitionFactory(
            input = GenerationInput(schemas = linkedMapOf(*schemas), polymorphicRelationships = emptyMap(), channels = emptyList()),
            packageName = "com.example.model",
        ).create()

    private fun inline(schema: Schema) = SchemaInterface.SchemaInline(schema)

    private fun ref(name: String) = SchemaInterface.SchemaReference(Reference("#/components/schemas/$name"))
}
