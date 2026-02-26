package dev.banking.asyncapi.generator.core.generator.avro.enumvalue

import dev.banking.asyncapi.generator.core.generator.AbstractAvroGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class AvroEnumTest : AbstractAvroGeneratorClass() {

    @Test
    fun `should generate strict avro enums`() {
        val content = generateAvro(
            yaml = File("src/test/resources/generator/asyncapi_enum_default_value.yaml"),
            packageName = "com.example.avro",
            schema = "Task.avsc"
        )

        assertTrue(content.isNotEmpty())

        assertTrue(content.contains("\"type\": [\"null\", \"com.example.avro.TaskStatus\"]"), "TaskStatus reference missing")
        assertTrue(content.contains("\"type\": [\"null\", \"com.example.avro.Priority\"]"), "Priority reference missing")

        val statusFile = File("target/generated-resources/asyncapi/com/example/avro/TaskStatus.avsc")
        assertTrue(statusFile.exists(), "TaskStatus.avsc missing")
        val statusContent = statusFile.readText()

        assertTrue(statusContent.contains("\"type\": \"enum\""), "TaskStatus should be enum")
        assertTrue(statusContent.contains("\"symbols\": ["), "TaskStatus symbols missing")
        assertTrue(statusContent.contains("\"OPEN\""), "Symbol OPEN missing")

        val priorityFile = File("target/generated-resources/asyncapi/com/example/avro/Priority.avsc")
        assertTrue(priorityFile.exists(), "Priority.avsc missing")
        val priorityContent = priorityFile.readText()

        assertTrue(priorityContent.contains("\"type\": \"enum\""), "Priority should be enum")
        assertTrue(priorityContent.contains("\"default\": \"MEDIUM\""), "Priority default missing")
    }
}
