package dev.banking.asyncapi.generator.core.registry

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import dev.banking.asyncapi.generator.core.constants.AsyncApiConstants.ROOT
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.serializers.AsyncApiListSerializer
import dev.banking.asyncapi.generator.core.serializers.AsyncApiStringSerializer
import java.io.File

object AsyncApiRegistry {

    fun readYaml(file: File, asyncApiContext: AsyncApiContext): ParserNode {
        val content = file.readText()
        asyncApiContext.registerSource(file, content)
        val fileId = buildFileId(file)
        val rootPath = "$fileId.$ROOT"
        val parsedYaml = YamlParserRegistry.parse(file.name, content, rootPath)
        parsedYaml.lineMappings.forEach { (path, line) ->
            asyncApiContext.registerLine(path, line)
        }
        return ParserNode(rootPath, parsedYaml.data, rootPath, asyncApiContext)
    }

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

    private fun buildFileId(file: File): String =
        file.nameWithoutExtension
            .replace(Regex("[^A-Za-z0-9_]"), "_")
}
