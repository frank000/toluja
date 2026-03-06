$ErrorActionPreference = "Stop"

param(
    [string]$Version = "dev"
)

$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $RepoRoot

$commit = "none"
try {
    $commit = (git rev-parse --short HEAD).Trim()
} catch {
    Write-Host "git commit nao encontrado; usando commit=none"
}

$env:VERSION = $Version
$env:GIT_COMMIT = $commit

& .\build.ps1

$packageDir = Join-Path $RepoRoot "dist\windows-release"
New-Item -ItemType Directory -Path $packageDir -Force | Out-Null

Copy-Item -LiteralPath ".\dist\print-agent-windows-amd64.exe" -Destination (Join-Path $packageDir "print-agent-windows-amd64.exe") -Force
Copy-Item -LiteralPath ".\.env.example" -Destination (Join-Path $packageDir ".env.example") -Force
Copy-Item -LiteralPath ".\windows\install-windows.ps1" -Destination (Join-Path $packageDir "install-windows.ps1") -Force
Copy-Item -LiteralPath ".\windows\uninstall-windows.ps1" -Destination (Join-Path $packageDir "uninstall-windows.ps1") -Force

Write-Host "Pacote Windows pronto em: $packageDir"
Write-Host "Arquivos incluidos: exe + .env.example + install/uninstall scripts"
