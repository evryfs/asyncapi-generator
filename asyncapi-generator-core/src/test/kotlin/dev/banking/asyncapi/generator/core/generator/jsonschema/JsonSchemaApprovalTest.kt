package dev.banking.asyncapi.generator.core.generator.jsonschema

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.fixtures.GeneratorApprovalFormat
import dev.banking.asyncapi.generator.core.fixtures.GeneratorApprovals
import org.junit.jupiter.api.Test

class JsonSchemaApprovalTest {
    private val generator = JsonSchemaGenerator()
    private val fixtures = GenerationInputFixtures()

    @Test
    fun approves_generated_json_schema() {
        val input = fixtures.generationInputWithJsonSchemas()
        val generated =
            generator
                .render(
                    schemaDeclarations = input.schemaDeclarations,
                    packageName = "com.example.schema",
                ).artifacts
                .single { artifact -> artifact.relativePath.endsWith("MyAccount.schema.json") }
                .content

        GeneratorApprovals.verify(
            generated = generated,
            format = GeneratorApprovalFormat.JSON_SCHEMA,
            scenario = "my-account",
        )
    }
}
