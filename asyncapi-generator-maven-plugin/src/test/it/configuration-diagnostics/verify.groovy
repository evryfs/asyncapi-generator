def buildLog = new File(basedir, "build.log")

assert buildLog.isFile() : "Expected Maven Invoker to retain the failed build log"
assert buildLog.text.contains(
    "No generator output is configured. Configure modelPackage, clientPackage with clientConfig, " +
        "schemaPackage with a schema generator, or outputFile.",
) : "Expected the generator configuration diagnostic before input-file validation"
assert !buildLog.text.contains("Input specification not found:") :
    "Configuration should be validated before the AsyncAPI input is read"

true
