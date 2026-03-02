package dev.banking.asyncapi.generator.core.generator.kafka.spring

import java.io.File

class AutoConfigurationResourceGenerator(
    private val resourceOutputDir: File,
) {
    fun generate(autoConfigClassName: String) {
        val file = File(resourceOutputDir, "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
        file.parentFile.mkdirs()
        val existing = if (file.exists()) file.readText().lines().filter { it.isNotBlank() } else emptyList()
        val updated = (existing + autoConfigClassName).distinct().joinToString("\n")
        file.writeText(updated + "\n")
    }
}
