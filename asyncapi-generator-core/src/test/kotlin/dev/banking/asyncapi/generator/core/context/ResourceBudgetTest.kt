package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.document.SourceLocation
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ResourceBudgetTest {

    private val budget = ResourceBudget(ParserLoadResourceLimits())

    @Test
    fun `sourceFiles is empty initially`() {
        assertTrue(budget.sourceFiles().isEmpty())
    }

    @Test
    fun `registerDocument returns success`() {
        val file = File.createTempFile("test", ".yaml")
        val location = SourceLocation("test", file, "", 1, 0)

        val result = budget.registerDocument(file, "content", location)

        assertIs<ResourceBudgetResult.Success<*>>(result)
        assertTrue(budget.sourceFiles().contains(file.canonicalFile))
    }

    @Test
    fun `registerExternalDocument returns limit exceeded when source document limit hit`() {
        val limits = ParserLoadResourceLimits(maxSourceDocuments = 1)
        val limitedBudget = ResourceBudget(limits)
        val file1 = File.createTempFile("ext1", ".yaml")
        val file2 = File.createTempFile("ext2", ".yaml")
        val location = SourceLocation("test", file1, "", 1, 0)

        limitedBudget.registerExternalDocument(file1, location)
        val result = limitedBudget.registerExternalDocument(file2, location)

        assertIs<ResourceBudgetResult.LimitExceeded>(result)
    }
}
