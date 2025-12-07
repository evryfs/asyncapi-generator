package com.tietoevry.banking.asyncapi.generator.core.bundler

import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import com.tietoevry.banking.asyncapi.generator.core.model.channels.ChannelInterface
import com.tietoevry.banking.asyncapi.generator.core.model.messages.MessageInterface
import com.tietoevry.banking.asyncapi.generator.core.parser.AsyncApiParser
import com.tietoevry.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import com.tietoevry.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class AsyncApiBundlerTest {

    private val asyncApiContext = AsyncApiContext()
    private val parser = AsyncApiParser(asyncApiContext)
    private val validator = AsyncApiValidator(asyncApiContext)
    private val bundler = AsyncApiBundler()

    @Test
    fun asyncApiSingleFile() {
        val file = File("src/test/resources/asyncapi_kafka_single_file_example.yaml")
        val root = AsyncApiRegistry.readYaml(file, asyncApiContext)
        val parsed = parser.parse(root)
        val validated = validator.validate(parsed)
        validated.apply {
            throwWarnings()
            throwErrors()
        }
        val result = bundler.bundle(parsed)
        val expected = expectedSingleFileBundled(file)
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expected)
    }

    @Test
    fun asyncApiMultiFile() {
        val file = File("src/test/resources/bundler/multi/asyncapi_multifile_example_main.yaml")
        val root = AsyncApiRegistry.readYaml(file, asyncApiContext)
        val result = parser.parse(root)
        val validated = validator.validate(result)
        validated.apply {
            throwWarnings()
            throwErrors()
        }
        val bundled = bundler.bundle(result)
        AsyncApiRegistry.writeYaml(File("src/test/resources/bundler/bundled/asyncapi-bundled.yaml"), bundled)
    }

    @Test
    fun asyncApiMultiFileAssertions() {
        val root = AsyncApiRegistry.readYaml(
            File("src/test/resources/bundler/multi/asyncapi_multifile_example_main.yaml"),
            asyncApiContext
        )
        val parsed = parser.parse(root)
        val validated = validator.validate(parsed)
        validated.apply {
            throwWarnings()
            throwErrors()
        }
        val result = bundler.bundle(parsed)
        val expected = expectedMultiFileBundled()
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expected)
    }

    @Test
    fun `bundling circular references should not cause stack overflow`() {
        val file = File("src/test/resources/bundler/circular/asyncapi_bundler_circular.yaml")
        val root = AsyncApiRegistry.readYaml(file, asyncApiContext)
        val parsed = parser.parse(root)
        val validated = validator.validate(parsed)
        assertFalse(validated.hasErrors(), "Expected no validation errors: ${validated.errors}")
        assertFalse(validated.hasWarnings(), "Expected no validation warnings: ${validated.warnings}")

        val bundled = bundler.bundle(parsed)
        assertNotNull(bundled, "Bundled document should not be null")
    }

    @Test
    fun `bundling marks references as inline`() {
        val file = File("src/test/resources/bundler/multi/asyncapi_multifile_example_main.yaml")
        val root = AsyncApiRegistry.readYaml(file, asyncApiContext)
        val parsed = parser.parse(root)
        val validated = validator.validate(parsed)
        assertFalse(validated.hasErrors(), "Expected no validation errors: ${validated.errors}")
        assertFalse(validated.hasWarnings(), "Expected no validation warnings: ${validated.warnings}")
        val bundled = bundler.bundle(parsed)

        val channel = bundled.channels!!["testChannel"] as ChannelInterface.ChannelInline
        val messageRef = channel.channel.messages!!["testMessage"] as MessageInterface.MessageReference

        assertThat(messageRef.reference.inline).isTrue()
        assertThat(messageRef.reference.model).isNotNull
    }

    private fun expectedSingleFileBundled(file: File): AsyncApiDocument {
        val root = AsyncApiRegistry.readYaml(file, asyncApiContext)
        val parsed = parser.parse(root)
        validator.validate(parsed)
        return parsed
    }
}
