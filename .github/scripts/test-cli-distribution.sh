#!/bin/sh
set -eu

archive=${1:?Usage: test-cli-distribution.sh ARCHIVE VERSION}
version=${2:?Usage: test-cli-distribution.sh ARCHIVE VERSION}
repo_root=$(CDPATH='' cd -- "$(dirname -- "$0")/../.." && pwd)
test_dir=$(mktemp -d)
trap 'rm -rf -- "$test_dir"' EXIT

mkdir -p "$test_dir/distribution with spaces ! & characters" "$test_dir/consumer"
tar -xzf "$archive" -C "$test_dir/distribution with spaces ! & characters"
distribution="$test_dir/distribution with spaces ! & characters/asyncapi-generator-$version"
test -x "$distribution/bin/asyncapi-generator"
test -f "$distribution/bin/asyncapi-generator.bat"
test -f "$distribution/lib/asyncapi-generator.jar"
test -f "$distribution/LICENSE"

cp "$repo_root/asyncapi-generator-cli/src/test/resources/asyncapi_spring_kafka.yaml" \
    "$test_dir/consumer/input contract.yaml"
export PATH="$distribution/bin:$PATH"
cd "$test_dir/consumer"

test "$(asyncapi-generator --version)" = "asyncapi-generator version $version"
asyncapi-generator --input-spec "input contract.yaml" --generator-name kotlin \
    --model-package com.example.smoke.model --output-directory "generated output"
grep -Fq 'data class MyAccountUpdatedPayload' \
    "generated output/com/example/smoke/model/MyAccountUpdatedPayload.kt"

if asyncapi-generator --not-an-option; then
    echo "The launcher did not preserve the CLI failure exit code." >&2
    exit 1
else
    test "$?" -eq 1
fi

test "$(unset JAVA_HOME; asyncapi-generator --version)" = "asyncapi-generator version $version"
echo "CLI distribution smoke test passed: $version"
