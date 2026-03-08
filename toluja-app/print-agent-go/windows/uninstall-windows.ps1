param(
    [string]$InstallDir = "$env:ProgramFiles\Toluja\PrintAgent",
    [string]$ServiceName = "TolujaPrintAgent",
    [string]$TaskName = "TolujaPrintAgent",
    [switch]$RemoveFiles
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $false

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

if (Get-Command Get-ScheduledTask -ErrorAction SilentlyContinue) {
    $existingTask = Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
    if ($null -ne $existingTask) {
        Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction SilentlyContinue | Out-Null
        Write-Host "Tarefa removida: $TaskName"
    }
} else {
    cmd.exe /c "schtasks.exe /Query /TN ""$TaskName"" >nul 2>nul"
    if ($LASTEXITCODE -eq 0) {
        schtasks.exe /Delete /TN $TaskName /F | Out-Null
        Write-Host "Tarefa removida: $TaskName"
    }
}

if ($RemoveFiles) {
    if (Test-Path -LiteralPath $InstallDir) {
        Remove-Item -LiteralPath $InstallDir -Recurse -Force
        Write-Host "Arquivos removidos: $InstallDir"
    }
}
