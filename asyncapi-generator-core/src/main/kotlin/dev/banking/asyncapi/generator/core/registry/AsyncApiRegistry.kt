package dev.banking.asyncapi.generator.core.registry

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.util.StringQuotingChecker
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import java.io.File
import org.yaml.snakeyaml.nodes.NodeId
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.resolver.Resolver

internal object AsyncApiRegistry {

    fun read(file: File, asyncApiContext: AsyncApiContext): ParserNode =
        ParserNodeFactory.root(
            document = DocumentReaderRegistry.read(file),
            context = asyncApiContext,
        )

    fun writeYaml(file: File, obj: Any) {
        val yamlText = serializeYaml(obj)
        file.parentFile?.mkdirs()
        file.writeText(yamlText)
        println("Yaml written to: ${file.absolutePath}")
    }

    fun writeJson(file: File, obj: Any) {
        val jsonText = serializeJson(obj)
        file.parentFile?.mkdirs()
        file.writeText(jsonText)
        println("Json written to: ${file.absolutePath}")
    }

    fun serializeYaml(obj: Any): String =
        yamlMapper.writeValueAsString(obj)

    fun serializeJson(obj: Any): String =
        jsonMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(obj)

    private val yamlMapper: ObjectMapper = ObjectMapper(
        YAMLFactory.builder()
            .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
            .configure(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR, true)
            .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
            .configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true)
            .stringQuotingChecker(ValuePreservingStringQuotingChecker)
            .build()
    ).apply {
        setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
    }

    private val jsonMapper: ObjectMapper =
        ObjectMapper()
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)

    private object ValuePreservingStringQuotingChecker : StringQuotingChecker() {
        private val default = Default.instance()
        private val resolver = Resolver()

        override fun needToQuoteName(name: String): Boolean =
            default.needToQuoteName(name)

        override fun needToQuoteValue(value: String): Boolean =
            default.needToQuoteValue(value) || resolver.resolve(NodeId.scalar, value, true) != Tag.STR
    }

}
