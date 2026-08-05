package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.fixtures.ResourceApprovalNamer
import dev.banking.asyncapi.generator.core.fixtures.testResourcesDirectory
import org.approvaltests.Approvals
import org.approvaltests.core.Options
import java.nio.file.Files

internal object BundlerApprovals {

    fun verify(
        generated: String,
        scenario: String,
    ) {
        val directory = testResourcesDirectory().resolve("approvals/bundler")
        Files.createDirectories(directory)
        val namer =
            ResourceApprovalNamer(
                directory = directory,
                scenario = scenario,
            )
        val options =
            Options()
                .forFile()
                .withExtension("yaml")
                .forFile()
                .withNamer(namer)

        Approvals.verify(generated, options)
    }
}
