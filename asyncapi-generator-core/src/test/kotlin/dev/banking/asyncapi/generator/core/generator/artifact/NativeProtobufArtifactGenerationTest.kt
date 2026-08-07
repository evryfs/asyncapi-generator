package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.configuration.ProtobufModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.ProtobufModelType
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NativeProtobufArtifactGenerationTest {
    private val generation = NativeProtobufArtifactGeneration()
    private val fixtures = GenerationInputFixtures()

    @Test
    fun `render returns relocated native Protobuf schema artifact`() {
        val result =
            generation.render(
                task =
                    GenerationTask.NativeProtobufArtifacts(
                        schemaPackageName = "com.example.schemas",
                    ),
                generationInput = fixtures.generationInputWithNativeProtobufSchema(),
            )

        assertEquals(
            listOf("com/example/schemas/UserCreated.proto" to GeneratedArtifactKind.SCHEMA),
            result.artifacts.map { it.relativePath to it.kind },
        )
    }

    @Test
    fun `render propagates Kotlin Protobuf model generation`() {
        val result =
            generation.render(
                task =
                    GenerationTask.NativeProtobufArtifacts(
                        models =
                            ProtobufModelGeneration(
                                packageName = "com.example.protobuf",
                                modelType = ProtobufModelType.KOTLIN,
                            ),
                    ),
                generationInput = fixtures.generationInputWithNativeProtobufJavaMessageSchema(),
            )

        assertEquals(
            GeneratedArtifactKind.JAVA_SOURCE,
            result.artifacts.single { it.relativePath == "com/example/protobuf/UserCreated.java" }.kind,
        )
        assertEquals(
            GeneratedArtifactKind.SOURCE,
            result.artifacts.single { it.relativePath == "com/example/protobuf/UserCreatedKt.kt" }.kind,
        )
    }
}
