package dev.banking.asyncapi.generator.core.fixtures

import org.approvaltests.Approvals
import org.approvaltests.core.Options
import java.nio.file.Files
import java.nio.file.Path

/**
 * Approval-test fixture for generated artifacts.
 *
 * Generator approval tests use this fixture to keep approved files under
 * `src/test/resources/approvals/generator` with the same file extension as the
 * generated artifact being approved.
 */
internal object GeneratorApprovals {
    fun verify(
        generated: String,
        format: GeneratorApprovalFormat,
        scenario: String,
    ) {
        val namer =
            ResourceApprovalNamer(
                directory = approvalDirectory(format),
                scenario = scenario,
            )
        val options =
            Options()
                .forFile()
                .withExtension(format.fileExtension)
                .forFile()
                .withNamer(namer)

        Approvals.verify(generated, options)
    }

    private fun approvalDirectory(format: GeneratorApprovalFormat): Path {
        val directory = testResourcesDirectory().resolve("approvals/generator/${format.directoryName}")
        Files.createDirectories(directory)
        return directory
    }
}

/**
 * Generated artifact format used by generator approval tests.
 */
internal enum class GeneratorApprovalFormat(
    val directoryName: String,
    val fileExtension: String,
) {
    JAVA("java", "java"),
    KOTLIN("kotlin", "kt"),
    AVRO("avro", "avsc"),
    NATIVE_AVRO_SCHEMA("native-avro/schema", "avsc"),
    NATIVE_AVRO_SPECIFIC_RECORD("native-avro/specific-record", "java"),
    NATIVE_PROTOBUF_SCHEMA("native-protobuf/schema", "proto"),
    JSON_SCHEMA("json-schema/schema", "json"),
    SPRING_KAFKA_JAVA("spring-kafka/java", "java"),
    SPRING_KAFKA_KOTLIN("spring-kafka/kotlin", "kt"),
}
