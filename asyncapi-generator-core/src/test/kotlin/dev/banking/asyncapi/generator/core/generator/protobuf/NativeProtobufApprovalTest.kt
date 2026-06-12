package dev.banking.asyncapi.generator.core.generator.protobuf

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.fixtures.GeneratorApprovalFormat
import dev.banking.asyncapi.generator.core.fixtures.GeneratorApprovals
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NativeProtobufApprovalTest {
    private val generator = NativeProtobufGenerator()
    private val fixtures = GenerationInputFixtures()

    @Test
    fun approves_native_protobuf_schema_artifact() {
        val generated =
            generator
                .render(fixtures.generationInputWithNativeProtobufSchema().multiFormatSchemas)
                .artifacts
                .single { artifact -> artifact.kind == GeneratedArtifactKind.SCHEMA }
                .content

        assertTrue(generated.isNotBlank())
        GeneratorApprovals.verify(
            generated = generated,
            format = GeneratorApprovalFormat.NATIVE_PROTOBUF_SCHEMA,
            scenario = "user-created",
        )
    }
}
