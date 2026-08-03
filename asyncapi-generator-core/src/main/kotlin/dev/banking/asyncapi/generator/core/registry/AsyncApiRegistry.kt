package dev.banking.asyncapi.generator.core.registry

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import dev.banking.asyncapi.generator.core.serializers.AsyncApiListSerializer
import dev.banking.asyncapi.generator.core.serializers.AsyncApiStringSerializer
import java.io.File

internal object AsyncApiRegistry {

    fun read(file: File, asyncApiContext: AsyncApiContext): ParserNode =
        ParserNodeFactory.root(
            document = DocumentReaderRegistry.read(file),
            context = asyncApiContext,
        )

    fun writeYaml(file: File, obj: Any) {
        val yamlText = yamlMapper.writeValueAsString(obj)
        file.parentFile?.mkdirs()
        file.writeText(yamlText)
        println("Yaml written to: ${file.absolutePath}")
    }

    fun writeJson(file: File, obj: Any) {
        val jsonText = jsonMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(obj)
        file.parentFile?.mkdirs()
        file.writeText(jsonText)
        println("Json written to: ${file.absolutePath}")
    }

    private val module = SimpleModule().apply {
        addSerializer(String::class.java, AsyncApiStringSerializer())
        addSerializer(List::class.java, AsyncApiListSerializer())
    }

    private val yamlMapper: ObjectMapper = ObjectMapper(
        YAMLFactory.builder()
            .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
            .configure(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR, true)
            .configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true)
            .build()
    ).apply {
        registerModule(module)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    private val jsonMapper: ObjectMapper =
        ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)

}
