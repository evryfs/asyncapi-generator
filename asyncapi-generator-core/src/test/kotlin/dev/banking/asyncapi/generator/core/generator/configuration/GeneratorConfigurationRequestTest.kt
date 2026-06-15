package dev.banking.asyncapi.generator.core.generator.configuration
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GeneratorConfigurationRequestTest {
    @Test
    fun `models request is created only when model output is configured`() {
        assertNull(GeneratorConfigurationRequest.models())
        assertNull(
            GeneratorConfigurationRequest.models(
                enabled = false,
                packageName = "com.example.model",
                annotation = "com.example.NoArg",
                javaModelType = "record",
            ),
        )

        assertEquals(
            GeneratorConfigurationRequest.Models(
                packageName = "com.example.model",
                annotation = "com.example.NoArg",
                javaModelType = JavaModelType.RECORD,
            ),
            GeneratorConfigurationRequest.models(
                packageName = "com.example.model",
                annotation = "com.example.NoArg",
                javaModelType = "record",
            ),
        )
    }

    @Test
    fun `avro projection request is created only when schema output is configured`() {
        assertNull(GeneratorConfigurationRequest.avroProjection())
        assertNull(
            GeneratorConfigurationRequest.avroProjection(
                enabled = false,
                packageName = "com.example.schema",
            ),
        )

        assertEquals(
            GeneratorConfigurationRequest.AvroProjection(packageName = "com.example.schema"),
            GeneratorConfigurationRequest.avroProjection(packageName = "com.example.schema"),
        )
    }

    @Test
    fun `native avro request is created only when schema output is configured`() {
        assertNull(GeneratorConfigurationRequest.nativeAvro())
        assertNull(
            GeneratorConfigurationRequest.nativeAvro(
                enabled = false,
                generateSpecificRecords = true,
            ),
        )

        assertEquals(
            GeneratorConfigurationRequest.NativeAvro(generateSpecificRecords = true),
            GeneratorConfigurationRequest.nativeAvro(enabled = true),
        )
        assertEquals(
            GeneratorConfigurationRequest.NativeAvro(generateSpecificRecords = false),
            GeneratorConfigurationRequest.nativeAvro(generateSpecificRecords = false),
        )
    }

    @Test
    fun `native protobuf request is created only when schema output is configured`() {
        assertNull(GeneratorConfigurationRequest.nativeProtobuf())
        assertNull(GeneratorConfigurationRequest.nativeProtobuf(enabled = false))

        assertEquals(
            GeneratorConfigurationRequest.NativeProtobuf(generateJavaMessageTypes = true),
            GeneratorConfigurationRequest.nativeProtobuf(enabled = true),
        )
        assertEquals(
            GeneratorConfigurationRequest.NativeProtobuf(generateJavaMessageTypes = false),
            GeneratorConfigurationRequest.nativeProtobuf(generateJavaMessageTypes = false),
        )
    }

    @Test
    fun `spring kafka request is created only when client output is configured`() {
        assertNull(GeneratorConfigurationRequest.springKafka())
        assertNull(
            GeneratorConfigurationRequest.springKafka(
                enabled = false,
                packageName = "com.example.client",
                modelPackageName = "com.example.model",
            ),
        )

        assertEquals(
            GeneratorConfigurationRequest.SpringKafka(
                packageName = "com.example.client",
                modelPackageName = "com.example.model",
            ),
            GeneratorConfigurationRequest.springKafka(
                packageName = "com.example.client",
                modelPackageName = "com.example.model",
            ),
        )
    }

    @Test
    fun `spring kafka request can be created from package only`() {
        assertEquals(
            GeneratorConfigurationRequest.SpringKafka(
                packageName = "com.example.client",
            ),
            GeneratorConfigurationRequest.springKafka(packageName = "com.example.client"),
        )
    }

    @Test
    fun `quarkus kafka request is created only when client output is configured`() {
        assertNull(GeneratorConfigurationRequest.quarkusKafka())
        assertNull(
            GeneratorConfigurationRequest.quarkusKafka(
                enabled = false,
                packageName = "com.example.client",
                modelPackageName = "com.example.model",
            ),
        )

        assertEquals(
            GeneratorConfigurationRequest.QuarkusKafka(
                packageName = "com.example.client",
                modelPackageName = "com.example.model",
            ),
            GeneratorConfigurationRequest.quarkusKafka(
                packageName = "com.example.client",
                modelPackageName = "com.example.model",
            ),
        )
    }
}
