param([string]$Version = '1.0.0')
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location -LiteralPath $projectRoot
$releaseDir = Join-Path $projectRoot 'app/build/outputs/apk/release'
$metadata = Get-Content -LiteralPath (Join-Path $releaseDir 'output-metadata.json') -Raw | ConvertFrom-Json
if ($metadata.applicationId -ne 'in.melo' -or $metadata.elements[0].versionName -ne $Version) {
    throw 'Build metadata does not match the requested Melo release.'
}
$apk = Join-Path $releaseDir $metadata.elements[0].outputFile
$sdkRoot = $env:ANDROID_HOME
if (-not $sdkRoot) { $sdkRoot = $env:ANDROID_SDK_ROOT }
if (-not $sdkRoot -and (Test-Path -LiteralPath 'local.properties')) {
    $sdkLine = Get-Content -LiteralPath 'local.properties' | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
    if ($sdkLine) { $sdkRoot = $sdkLine.Substring(8).Replace('\\', '\').Replace('\:', ':') }
}
if (-not $sdkRoot) { throw 'Set ANDROID_HOME or sdk.dir in local.properties.' }
$buildTools = Get-ChildItem -LiteralPath (Join-Path $sdkRoot 'build-tools') -Directory |
    Where-Object Name -Match '^\d+\.\d+\.\d+$' | Sort-Object { [version]$_.Name } | Select-Object -Last 1
$certificate = & (Join-Path $buildTools.FullName 'apksigner.bat') verify --verbose --print-certs $apk
if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
$dist = Join-Path $projectRoot 'dist'
New-Item -ItemType Directory -Path $dist -Force | Out-Null
$name = "Melo-v$Version.apk"
$target = Join-Path $dist $name
Copy-Item -LiteralPath $apk -Destination $target
$checksum = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant()
"$checksum  $name" | Set-Content -LiteralPath (Join-Path $dist "$name.sha256") -Encoding ASCII
$certificate | Set-Content -LiteralPath (Join-Path $dist 'Melo-SIGNING-CERTIFICATE.txt') -Encoding ASCII
Write-Output "Packaged $name with a verified signature."
Write-Output "SHA-256: $checksum"
