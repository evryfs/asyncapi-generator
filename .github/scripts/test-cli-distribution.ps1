param(
    [Parameter(Mandatory = $true)][string]$Archive,
    [Parameter(Mandatory = $true)][string]$Version
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$testDir = Join-Path ([System.IO.Path]::GetTempPath()) ([guid]::NewGuid().ToString())
$originalLocation = Get-Location
$originalPath = $env:PATH
$originalJavaHome = $env:JAVA_HOME

try {
    $unpackDir = Join-Path $testDir 'distribution with spaces ! & characters'
    $consumerDir = Join-Path $testDir 'consumer'
    New-Item -ItemType Directory -Path $unpackDir, $consumerDir | Out-Null
    Expand-Archive -LiteralPath $Archive -DestinationPath $unpackDir
    $distribution = Join-Path $unpackDir "asyncapi-generator-$Version"
    foreach ($file in 'bin/asyncapi-generator', 'bin/asyncapi-generator.bat', 'lib/asyncapi-generator.jar', 'LICENSE') {
        if (-not (Test-Path -LiteralPath (Join-Path $distribution $file) -PathType Leaf)) {
            throw "Missing distribution file: $file"
        }
    }

    Copy-Item -LiteralPath (Join-Path $repoRoot 'asyncapi-generator-cli/src/test/resources/asyncapi_spring_kafka.yaml') `
        -Destination (Join-Path $consumerDir 'input contract.yaml')
    $env:PATH = "$(Join-Path $distribution 'bin');$env:PATH"
    Set-Location -LiteralPath $consumerDir

    $output = & asyncapi-generator --version
    if ($LASTEXITCODE -ne 0 -or $output -ne "asyncapi-generator version $Version") {
        throw "Unexpected PowerShell version result: $output (exit $LASTEXITCODE)"
    }
    & asyncapi-generator --input-spec 'input contract.yaml' --generator-name kotlin `
        --model-package com.example.smoke.model --output-directory 'generated output'
    if ($LASTEXITCODE -ne 0) { throw "PowerShell generation failed: $LASTEXITCODE" }
    $model = Join-Path $consumerDir 'generated output/com/example/smoke/model/MyAccountUpdatedPayload.kt'
    if (-not (Select-String -LiteralPath $model -SimpleMatch 'data class MyAccountUpdatedPayload' -Quiet)) {
        throw 'PowerShell generation did not produce the expected model.'
    }

    $output = & $env:ComSpec /d /v:off /c 'asyncapi-generator --version'
    if ($LASTEXITCODE -ne 0 -or $output -ne "asyncapi-generator version $Version") {
        throw "Unexpected Command Prompt version result: $output (exit $LASTEXITCODE)"
    }
    & $env:ComSpec /d /v:off /c 'asyncapi-generator --input-spec "input contract.yaml" --generator-name kotlin --model-package com.example.smoke.model --output-directory "cmd generated output"'
    if ($LASTEXITCODE -ne 0) { throw "Command Prompt generation failed: $LASTEXITCODE" }
    $model = Join-Path $consumerDir 'cmd generated output/com/example/smoke/model/MyAccountUpdatedPayload.kt'
    if (-not (Select-String -LiteralPath $model -SimpleMatch 'data class MyAccountUpdatedPayload' -Quiet)) {
        throw 'Command Prompt generation did not produce the expected model.'
    }

    & asyncapi-generator --not-an-option
    if ($LASTEXITCODE -ne 1) { throw "The launcher did not preserve the CLI failure exit code: $LASTEXITCODE" }

    $env:JAVA_HOME = $null
    $output = & asyncapi-generator --version
    if ($LASTEXITCODE -ne 0 -or $output -ne "asyncapi-generator version $Version") {
        throw "Unexpected PATH Java version result: $output (exit $LASTEXITCODE)"
    }
    Write-Host "CLI distribution smoke test passed: $Version"
}
finally {
    Set-Location -LiteralPath $originalLocation.Path
    $env:PATH = $originalPath
    $env:JAVA_HOME = $originalJavaHome
    Remove-Item -LiteralPath $testDir -Recurse -Force
}
