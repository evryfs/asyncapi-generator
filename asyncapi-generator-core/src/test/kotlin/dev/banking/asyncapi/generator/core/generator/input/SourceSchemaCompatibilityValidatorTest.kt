package dev.banking.asyncapi.generator.core.generator.input

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedSourceSchemaFeature
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SourceSchemaCompatibilityValidatorTest {
    @Test
    fun `rejects tuple form items for structural model output`() {
        val error =
            assertFailsWith<UnsupportedSourceSchemaFeature> {
                validate(
                    schemas =
                        mapOf(
                            "Event" to
                                Schema(
                                    tupleItems =
                                        listOf(
                                            inline(Schema(type = "string")),
                                            inline(Schema(type = "integer")),
                                        ),
                                ),
                        ),
                )
            }

        assertEquals(
            """

            Model generation cannot represent schema 'Event'.
            Incompatible schema path: $
            Incompatible feature: tuple-form 'items'.
            Use a compatible schema shape for this output, or remove the output from generator configuration.
            """.trimIndent(),
            error.message,
        )
    }

    @Test
    fun `rejects false items for structural model output`() {
        val error =
            assertFailsWith<UnsupportedSourceSchemaFeature> {
                validate(
                    schemas =
                        mapOf(
                            "Event" to
                                Schema(
                                    type = "array",
                                    items = SchemaInterface.BooleanSchema(false),
                                ),
                        ),
                )
            }

        assertEquals("Incompatible feature: 'items: false'.", error.message!!.lineSequence().elementAt(3))
    }

    @Test
    fun `rejects untyped enum containing a non string value for structural model output`() {
        val error =
            assertFailsWith<UnsupportedSourceSchemaFeature> {
                validate(
                    schemas = mapOf("Status" to Schema(enum = listOf("ACTIVE", 2))),
                )
            }

        assertEquals(
            "Incompatible feature: an enum without 'type' that contains non-string values.",
            error.message!!.lineSequence().elementAt(3),
        )
    }

    @Test
    fun `rejects a pattern that Java cannot compile when Java patterns are checked`() {
        val error =
            assertFailsWith<UnsupportedSourceSchemaFeature> {
                validate(
                    schemas = mapOf("Identifier" to Schema(type = "string", pattern = "[")),
                    checkStructuralModels = false,
                    checkJavaPatterns = true,
                )
            }

        assertEquals(
            "Incompatible feature: a 'pattern' that Java cannot compile: Unclosed character class.",
            error.message!!.lineSequence().elementAt(3),
        )
    }

    @Test
    fun `reports the first root and exact nested inline schema path`() {
        val error =
            assertFailsWith<UnsupportedSourceSchemaFeature> {
                validate(
                    schemas =
                        linkedMapOf(
                            "First" to Schema(type = "object"),
                            "Second" to
                                Schema(
                                    definitions =
                                        linkedMapOf(
                                            "Address" to
                                                inline(
                                                    Schema(
                                                        anyOf =
                                                            listOf(
                                                                inline(Schema(type = "string")),
                                                                inline(Schema(items = SchemaInterface.BooleanSchema(false))),
                                                            ),
                                                    ),
                                                ),
                                        ),
                                ),
                        ),
                )
            }

        assertEquals(
            """

            Model generation cannot represent schema 'Second'.
            Incompatible schema path: $.definitions['Address'].anyOf[1]
            Incompatible feature: 'items: false'.
            Use a compatible schema shape for this output, or remove the output from generator configuration.
            """.trimIndent(),
            error.message,
        )
    }

    @Test
    fun `traverses all inline schema fields`() {
        val valid = inline(Schema(type = "string"))
        val invalid = inline(Schema(pattern = "["))
        val schema =
            Schema(
                items = valid,
                tupleItems = emptyList(),
                additionalItems = valid,
                contains = valid,
                properties = mapOf("property" to valid),
                patternProperties = mapOf("pattern" to valid),
                additionalProperties = valid,
                propertyNames = valid,
                dependencies = mapOf("dependency" to valid),
                definitions = mapOf("definition" to valid),
                allOf = listOf(valid),
                anyOf = listOf(valid),
                oneOf = listOf(valid),
                not = valid,
                ifSchema = valid,
                thenSchema = valid,
                elseSchema = invalid,
            )

        val error =
            assertFailsWith<UnsupportedSourceSchemaFeature> {
                validate(
                    schemas = mapOf("Event" to schema),
                    checkStructuralModels = false,
                    checkJavaPatterns = true,
                )
            }

        assertEquals("Incompatible schema path: $.else", error.message!!.lineSequence().elementAt(2))
    }

    @Test
    fun `handles inline identity cycles`() {
        val properties = linkedMapOf<String, SchemaInterface>()
        val schema = Schema(type = "object", properties = properties)
        properties["self"] = inline(schema)

        validate(schemas = mapOf("Recursive" to schema))
    }

    private fun validate(
        schemas: Map<String, Schema>,
        checkStructuralModels: Boolean = true,
        checkJavaPatterns: Boolean = false,
    ) {
        SourceSchemaCompatibilityValidator.validate(
            output = "Model generation",
            schemas = schemas,
            checkStructuralModels = checkStructuralModels,
            checkJavaPatterns = checkJavaPatterns,
        )
    }

    private fun inline(schema: Schema): SchemaInterface = SchemaInterface.SchemaInline(schema)
}
