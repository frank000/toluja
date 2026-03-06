$ErrorActionPreference = "Stop"

param(
    [string]$InstallDir = "$env:ProgramFiles\Toluja\PrintAgent",
    [string]$ServiceName = "TolujaPrintAgent",
    [string]$TaskName = "TolujaPrintAgent",
    [switch]$RemoveFiles
)

function Test-IsAdmin {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltinRole]::Administrator)
}

if (-not (Test-IsAdmin)) {
    throw "Execute este script como Administrador."
}

sc.exe query $ServiceName *> $null
if ($LASTEXITCODE -eq 0) {
    sc.exe stop $ServiceName | Out-Null
    Start-Sleep -Seconds 2
    sc.exe delete $ServiceName | Out-Null
    Write-Host "Servico removido: $ServiceName"
}

schtasks.exe /Query /TN $TaskName *> $null
if ($LASTEXITCODE -eq 0) {
    schtasks.exe /Delete /TN $TaskName /F | Out-Null
    Write-Host "Tarefa removida: $TaskName"
}

if ($RemoveFiles) {
    if (Test-Path -LiteralPath $InstallDir) {
        Remove-Item -LiteralPath $InstallDir -Recurse -Force
        Write-Host "Arquivos removidos: $InstallDir"
    }
}
