param(
    [string]$Version = "dev"
)

$ErrorActionPreference = "Stop"

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
$oneClickDir = Join-Path $packageDir "oneclick-installer"
New-Item -ItemType Directory -Path $oneClickDir -Force | Out-Null

Copy-Item -LiteralPath ".\dist\print-agent-windows-amd64.exe" -Destination (Join-Path $packageDir "print-agent-windows-amd64.exe") -Force
Copy-Item -LiteralPath ".\.env.example" -Destination (Join-Path $packageDir ".env.example") -Force
Copy-Item -LiteralPath ".\windows\install-windows.ps1" -Destination (Join-Path $packageDir "install-windows.ps1") -Force
Copy-Item -LiteralPath ".\windows\uninstall-windows.ps1" -Destination (Join-Path $packageDir "uninstall-windows.ps1") -Force
Copy-Item -LiteralPath ".\dist\print-agent-windows-amd64.exe" -Destination (Join-Path $oneClickDir "toluja-print-agent.exe") -Force
Copy-Item -LiteralPath ".\windows-oneclick\Instalar Impressora Toluja.bat" -Destination (Join-Path $oneClickDir "Instalar Impressora Toluja.bat") -Force
Copy-Item -LiteralPath ".\windows-oneclick\install-windows.ps1" -Destination (Join-Path $oneClickDir "install-windows.ps1") -Force
if (Test-Path ".\.env") {
    Copy-Item -LiteralPath ".\.env" -Destination (Join-Path $oneClickDir ".env") -Force
} else {
    Copy-Item -LiteralPath ".\.env.example" -Destination (Join-Path $oneClickDir ".env") -Force
}

Write-Host "Pacote Windows pronto em: $packageDir"
Write-Host "Arquivos incluidos: exe + .env.example + install/uninstall scripts + oneclick-installer"
