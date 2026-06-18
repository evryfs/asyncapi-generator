package dev.banking.asyncapi.generator.core.fixtures

import org.approvaltests.Approvals
import org.approvaltests.core.Options
import org.approvaltests.namer.ApprovalNamer
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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
            GeneratorApprovalNamer(
                approvalDirectory = approvalDirectory(format),
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

    private fun testResourcesDirectory(): Path {
        val moduleDirectory = Paths.get("src/test/resources")
        if (Files.exists(moduleDirectory)) {
            return moduleDirectory
        }

        val rootDirectory = Paths.get("asyncapi-generator-core/src/test/resources")
        if (Files.exists(rootDirectory)) {
            return rootDirectory
        }

        return moduleDirectory
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
}

private class GeneratorApprovalNamer(
    private val approvalDirectory: Path,
    private val scenario: String,
    private val additionalInformation: String = "",
) : ApprovalNamer {
    override fun getApprovedFile(fileExtensionWithDot: String): File =
        approvalFile("approved", fileExtensionWithDot)

    override fun getReceivedFile(fileExtensionWithDot: String): File =
        approvalFile("received", fileExtensionWithDot)

    override fun getApprovalName(): String =
        approvalBaseName()

    override fun getSourceFilePath(): String =
        approvalDirectory.toString()

    override fun addAdditionalInformation(additionalInformation: String): ApprovalNamer =
        GeneratorApprovalNamer(
            approvalDirectory = approvalDirectory,
            scenario = scenario,
            additionalInformation =
                listOf(this.additionalInformation, additionalInformation)
                    .filter { it.isNotBlank() }
                    .joinToString("."),
        )

    override fun getAdditionalInformation(): String =
        additionalInformation

    private fun approvalFile(
        approvalState: String,
        fileExtensionWithDot: String,
    ): File =
        approvalDirectory
            .resolve("${approvalBaseName()}.$approvalState$fileExtensionWithDot")
            .toFile()

    private fun approvalBaseName(): String =
        listOf(scenario, additionalInformation)
            .filter { it.isNotBlank() }
            .joinToString(".")
}
