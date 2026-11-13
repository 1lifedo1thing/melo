# Create once. Back up .signing/ securely; it is never included in Git.
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$signingDir = Join-Path $projectRoot '.signing'
$keyFile = Join-Path $signingDir 'melo-release.jks'
$propertiesFile = Join-Path $signingDir 'melo-release.properties'
if ((Test-Path -LiteralPath $keyFile) -or (Test-Path -LiteralPath $propertiesFile)) {
    throw 'Signing material already exists. Reuse and back it up; do not replace it.'
}
New-Item -ItemType Directory -Path $signingDir -Force | Out-Null
$randomBytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($randomBytes) } finally { $rng.Dispose() }
$env:MELO_SIGNING_PASSWORD = [Convert]::ToBase64String($randomBytes)
try {
    $keytool = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/keytool.exe' } else { (Get-Command keytool.exe).Source }
    & $keytool -genkeypair -noprompt -keystore $keyFile -storetype JKS -alias melo `
        -storepass:env MELO_SIGNING_PASSWORD -keypass:env MELO_SIGNING_PASSWORD `
        -keyalg RSA -keysize 4096 -validity 10000 -dname 'CN=Melo, OU=Android, O=Melo'
    if ($LASTEXITCODE -ne 0) { throw 'Signing key generation failed.' }
    @(
        'storeFile=.signing/melo-release.jks'
        "storePassword=$env:MELO_SIGNING_PASSWORD"
        'keyAlias=melo'
        "keyPassword=$env:MELO_SIGNING_PASSWORD"
    ) | Set-Content -LiteralPath $propertiesFile -Encoding ASCII
} finally { Remove-Item Env:MELO_SIGNING_PASSWORD }
Write-Output 'Release signing configured. Back up .signing/ securely before publishing updates.'
