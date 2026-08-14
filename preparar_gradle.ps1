$ErrorActionPreference = "Stop"

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$wrapperDir = Join-Path $projectDir "gradle\wrapper"
$wrapperJar = Join-Path $wrapperDir "gradle-wrapper.jar"
$url = "https://github.com/gradle/gradle/raw/refs/tags/v9.4.1/gradle/wrapper/gradle-wrapper.jar"
$expected = "55243ef57851f12b070ad14f7f5bb8302daceeebc5bce5ece5fa6edb23e1145c"

New-Item -ItemType Directory -Force -Path $wrapperDir | Out-Null
Write-Host "Baixando Gradle Wrapper 9.4.1 do repositório oficial do Gradle..."
Invoke-WebRequest -Uri $url -OutFile $wrapperJar

$actual = (Get-FileHash -Path $wrapperJar -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actual -ne $expected) {
    Remove-Item $wrapperJar -Force
    throw "Checksum do Gradle Wrapper não confere. Arquivo removido por segurança."
}

Write-Host "Gradle Wrapper verificado com sucesso."
Write-Host "Agora abra a pasta do projeto no Android Studio."
