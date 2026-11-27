package dev.banking.asyncapi.generator.core.generator.protobuf

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
