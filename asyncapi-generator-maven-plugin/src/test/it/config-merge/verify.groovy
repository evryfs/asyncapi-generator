def codegenBase = new File(basedir, "target/generated-sources/asyncapi")

def aProducerDir = new File(codegenBase, "com/example/a/client/producer")
def aConsumerDir = new File(codegenBase, "com/example/a/client/consumer")
def bProducerDir = new File(codegenBase, "com/example/b/client/producer")
def bConsumerDir = new File(codegenBase, "com/example/b/client/consumer")
def aModelDir = new File(codegenBase, "com/example/a/model")
def bModelDir = new File(codegenBase, "com/example/b/model")

assert aModelDir.exists() : "Expected model directory for contract A"
assert bModelDir.exists() : "Expected model directory for contract B"
assert aProducerDir.exists() : "Expected inherited producer configuration for contract A"
assert aConsumerDir.exists() : "Expected inherited consumer configuration for contract A"
assert bProducerDir.exists() : "Expected inherited producer configuration for contract B"
assert !bConsumerDir.exists() : "Did not expect disabled consumer generation for contract B"
