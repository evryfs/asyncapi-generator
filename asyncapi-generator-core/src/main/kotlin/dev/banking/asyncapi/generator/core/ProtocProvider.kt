package dev.banking.asyncapi.generator.core

import dev.banking.asyncapi.generator.core.generator.protobuf.MavenRepositoryProtocProvider
import dev.banking.asyncapi.generator.core.generator.protobuf.ProtocPlatform
import java.nio.file.Path

/**
 * Supplies the `protoc` compiler that turns native Protobuf schemas into Java and Kotlin sources.
 *
 * The compiler is the official `com.google.protobuf:protoc` executable from Maven repositories.
 * Build plugins resolve it the way their build tool resolves artifacts, so repository mirrors,
 * proxies and credentials apply; other callers use [fromMavenRepository] or [executable].
 */
fun interface ProtocProvider {
    /** Returns an executable `protoc` for [version], resolving it on first use. */
    fun protoc(version: String): Path

    companion object {
        /** The protoc version matching the Protobuf runtime the generated sources need. */
        const val DEFAULT_VERSION = "3.25.5"

        /**
         * Takes `protoc` from the local Maven repository, downloading it from Maven Central into that
         * repository first when it is missing.
         */
        fun fromMavenRepository(): ProtocProvider = MavenRepositoryProtocProvider()

        /** Always uses the `protoc` executable at [path], which must exist. */
        fun executable(path: Path): ProtocProvider = MavenRepositoryProtocProvider(override = path)

        /** The Maven classifier of the `protoc` executable for this machine, such as `linux-x86_64`. */
        fun platformClassifier(): String = ProtocPlatform.classifier()
    }
}
