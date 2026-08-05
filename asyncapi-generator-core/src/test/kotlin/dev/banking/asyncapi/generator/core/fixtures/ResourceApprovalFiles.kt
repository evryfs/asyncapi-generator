package dev.banking.asyncapi.generator.core.fixtures

import org.approvaltests.namer.ApprovalNamer
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal fun testResourcesDirectory(): Path {
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

internal class ResourceApprovalNamer(
    private val directory: Path,
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
        directory.toString()

    override fun addAdditionalInformation(additionalInformation: String): ApprovalNamer =
        ResourceApprovalNamer(
            directory = directory,
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
        directory
            .resolve("${approvalBaseName()}.$approvalState$fileExtensionWithDot")
            .toFile()

    private fun approvalBaseName(): String =
        listOf(scenario, additionalInformation)
            .filter { it.isNotBlank() }
            .joinToString(".")
}
