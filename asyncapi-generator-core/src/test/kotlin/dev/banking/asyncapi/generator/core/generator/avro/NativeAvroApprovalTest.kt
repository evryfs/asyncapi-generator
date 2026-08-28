package dev.banking.asyncapi.generator.core.generator.avro

import dev.banking.asyncapi.generator.core.fixtures.BundlerFixtures
import dev.banking.asyncapi.generator.core.fixtures.GeneratorApprovalFormat
import dev.banking.asyncapi.generator.core.fixtures.GeneratorApprovals
import dev.banking.asyncapi.generator.core.generator.input.GenerationInputFactory
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NativeAvroApprovalTest {
    private val generator = NativeAvroGenerator()
    private val bundlerFixtures = BundlerFixtures()
    private val generationInputFactory = GenerationInputFactory()

    @Test
    fun approves_native_avro_schema_artifact() {
        val generated = nativeAvroArtifacts().singleArtifact(GeneratedArtifactKind.SCHEMA).content

        assertTrue(generated.isNotBlank())
        GeneratorApprovals.verify(
            generated = generated,
            format = GeneratorApprovalFormat.NATIVE_AVRO_SCHEMA,
            scenario = "user-created",
        )
    }

    @Test
    fun approves_native_avro_specific_record_artifact() {
        val generated = nativeAvroArtifacts().singleArtifact(GeneratedArtifactKind.JAVA_SOURCE).content

        assertTrue(generated.isNotBlank())
        GeneratorApprovals.verify(
            generated = generated,
            format = GeneratorApprovalFormat.NATIVE_AVRO_SPECIFIC_RECORD,
            scenario = "user-created",
        )
    }

    private fun nativeAvroArtifacts(): List<GeneratedArtifact> =
        generator
            .render(
                schemas =
                    generationInputFactory
                        .create(
                            bundlerFixtures.bundledDocument(
                                "generator/asyncapi_native_avro_spring_kafka_client.yaml",
                            ),
                        ).multiFormatSchemas,
                generateSpecificRecords = true,
            ).artifacts

    private fun List<GeneratedArtifact>.singleArtifact(kind: GeneratedArtifactKind): GeneratedArtifact =
        single { artifact -> artifact.kind == kind }
}
