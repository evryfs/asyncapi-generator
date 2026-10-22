#!/bin/sh
set -eu

version=${1:?Usage: test-published-artifacts.sh VERSION DESTINATION}
destination=${2:?Usage: test-published-artifacts.sh VERSION DESTINATION}
repo_root=$(CDPATH='' cd -- "$(dirname -- "$0")/../.." && pwd)
test_dir=$(mktemp -d)
trap 'rm -rf -- "$test_dir"' EXIT
central=https://repo.maven.apache.org/maven2
group_path=dev/banking/asyncapi/generator

# Publication completion can precede availability at a public repository edge.
probe() {
    deadline=$(($(date +%s) + 600))
    while :; do
        if resolved_url=$(curl --head --location --fail --silent --show-error --max-time 30 \
            --output /dev/null --write-out '%{url_effective}' "$1"); then
            if [ "$destination" != gradle-portal ]; then
                printf '%s' "$resolved_url"
                return 0
            fi
            case "$resolved_url" in
                https://plugins.gradle.org/*|https://plugins-artifacts.gradle.org/*)
                    printf '%s' "$resolved_url"
                    return 0
                    ;;
                *) echo "Waiting for Plugin Portal publication; artifact currently resolves to $resolved_url" >&2 ;;
            esac
        fi
        if [ "$(date +%s)" -ge "$deadline" ]; then
            echo "Published artifact is unavailable from $destination: $1" >&2
            return 1
        fi
        sleep 15
    done
}

mkdir "$test_dir/consumer"
cp "$repo_root/asyncapi-generator-cli/src/test/resources/asyncapi_spring_kafka.yaml" "$test_dir/consumer/api.yaml"
cd "$test_dir/consumer"

case "$destination" in
    maven)
        for artifact in asyncapi-generator asyncapi-generator-core asyncapi-generator-cli asyncapi-generator-maven-plugin; do
            probe "$central/$group_path/$artifact/$version/$artifact-$version.pom" > /dev/null
            if [ "$artifact" != asyncapi-generator ]; then
                probe "$central/$group_path/$artifact/$version/$artifact-$version.jar" > /dev/null
            fi
        done
        cp "$repo_root/.github/release-smoke/maven/pom.xml" pom.xml
        cat > "$test_dir/settings.xml" <<'EOF'
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
  <mirrors>
    <mirror>
      <id>public-central</id><mirrorOf>*</mirrorOf>
      <url>https://repo.maven.apache.org/maven2</url>
    </mirror>
  </mirrors>
</settings>
EOF
        mvn -B --settings "$test_dir/settings.xml" --global-settings "$test_dir/settings.xml" \
            -Dmaven.repo.local="$test_dir/maven-cache" -Dgenerator.version="$version" \
            "dev.banking.asyncapi.generator:asyncapi-generator-maven-plugin:$version:generate"
        grep -Fq 'data class MyAccountUpdatedPayload' \
            target/generated/com/example/smoke/model/MyAccountUpdatedPayload.kt
        test -f "$test_dir/maven-cache/$group_path/asyncapi-generator/$version/asyncapi-generator-$version.pom"
        test -f "$test_dir/maven-cache/$group_path/asyncapi-generator-core/$version/asyncapi-generator-core-$version.jar"

        curl --location --fail --silent --show-error \
            "$central/$group_path/asyncapi-generator-cli/$version/asyncapi-generator-cli-$version.jar" \
            --output "$test_dir/cli.jar"
        test "$(java -jar "$test_dir/cli.jar" --version)" = "asyncapi-generator version $version"
        java -jar "$test_dir/cli.jar" --input-spec api.yaml --generator-name kotlin \
            --model-package com.example.smoke.model --output-directory cli-output
        grep -Fq 'data class MyAccountUpdatedPayload' \
            cli-output/com/example/smoke/model/MyAccountUpdatedPayload.kt
        ;;
    gradle-central|gradle-portal)
        if [ "$destination" = gradle-portal ]; then
            repository=https://plugins.gradle.org/m2
        else
            repository=$central
        fi
        for component in \
            "asyncapi-generator-gradle-plugin/$version/asyncapi-generator-gradle-plugin-$version.pom" \
            "asyncapi-generator-gradle-plugin/$version/asyncapi-generator-gradle-plugin-$version.jar" \
            "dev.banking.asyncapi.generator.gradle.plugin/$version/dev.banking.asyncapi.generator.gradle.plugin-$version.pom"; do
            probe "$repository/$group_path/$component" > /dev/null
        done
        cp "$repo_root/.github/release-smoke/gradle/"*.gradle.kts .
        export SMOKE_DESTINATION="$destination"
        export GRADLE_USER_HOME="$test_dir/gradle-cache"
        "$repo_root/asyncapi-generator-gradle-plugin/gradlew" --no-daemon \
            --project-dir "$test_dir/consumer" -PgeneratorVersion="$version" generateAsyncApi
        grep -Fq 'data class MyAccountUpdatedPayload' \
            build/generated/asyncapi/models/com/example/smoke/model/MyAccountUpdatedPayload.kt
        ;;
    *) echo "Unknown publication destination: $destination" >&2; exit 1 ;;
esac

echo "Published-artifact smoke test passed: $destination $version"
