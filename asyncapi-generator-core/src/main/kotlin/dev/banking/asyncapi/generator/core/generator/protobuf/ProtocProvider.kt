package dev.banking.asyncapi.generator.core.generator.protobuf

import java.nio.file.Path

/**
 * Supplies the `protoc` compiler that turns native Protobuf schemas into Java and Kotlin sources.
 *
 * The compiler is the official `com.google.protobuf:protoc` executable from Maven repositories.
 * Frontends resolve it the way their build tool resolves artifacts, so repository mirrors,
 * proxies and credentials apply; the CLI uses [MavenRepositoryProtocProvider].
 */
fun interface ProtocProvider {
    /** Returns an executable `protoc` for [version], resolving it on first use. */
    fun protoc(version: String): Path

    companion object {
        /** The protoc version matching the Protobuf runtime the generated sources need. */
        const val DEFAULT_VERSION = "3.25.5"
    }
}

/**
 * The Maven classifier of the `protoc` executable for this machine, such as `linux-x86_64`.
 */
object ProtocPlatform {
    fun classifier(
        osName: String = System.getProperty("os.name"),
        osArch: String = System.getProperty("os.arch"),
    ): String = "${operatingSystem(osName)}-${architecture(osArch)}"

    private fun operatingSystem(osName: String): String {
        val name = osName.lowercase()
        return when {
            name.startsWith("linux") -> "linux"
            name.startsWith("mac") || name.startsWith("darwin") -> "osx"
            name.startsWith("windows") -> "windows"
            else -> throw UnsupportedOperationException("protoc is not published for operating system '$osName'")
        }
    }

    private fun architecture(osArch: String): String =
        when (osArch.lowercase()) {
            "amd64", "x86_64", "x64" -> "x86_64"
            "aarch64", "arm64" -> "aarch_64"
            "x86", "i386", "i486", "i586", "i686" -> "x86_32"
            "ppc64le" -> "ppcle_64"
            "s390x" -> "s390_64"
            else -> throw UnsupportedOperationException("protoc is not published for architecture '$osArch'")
        }
}
