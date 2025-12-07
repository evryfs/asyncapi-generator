package com.tietoevry.banking.asyncapi.generator.core.generator.util

import java.io.File

object FileUtil {

    fun packageDirectory(baseDir: File, packageName: String): File {
        val packagePath = packageName.replace('.', File.separatorChar)
        val dir = baseDir.resolve(packagePath)
        dir.mkdirs()
        return dir
    }
}
