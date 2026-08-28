package dev.banking.asyncapi.generator.core.generator.protobuf

import dev.banking.asyncapi.generator.core.fixtures.BundlerFixtures
import dev.banking.asyncapi.generator.core.fixtures.GeneratorApprovalFormat
import dev.banking.asyncapi.generator.core.fixtures.GeneratorApprovals
import dev.banking.asyncapi.generator.core.generator.input.GenerationInputFactory
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NativeProtobufApprovalTest {
    private val generator = NativeProtobufGenerator()
    private val bundlerFixtures = BundlerFixtures()
    private val generationInputFactory = GenerationInputFactory()

    @Test
    fun approves_native_protobuf_schema_artifact() {
        val generated =
            generator
                .render(
                    generationInputFactory
                        .create(
                            bundlerFixtures.bundledDocument(
                                "examples/generated-output/native-protobuf.yaml",
                            ),
                        ).multiFormatSchemas,
                )
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
