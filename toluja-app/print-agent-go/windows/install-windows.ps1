param(
    [ValidateSet("service", "startup")]
    [string]$Mode = "service",
    [string]$SourceExe = ".\print-agent-windows-amd64.exe",
    [string]$InstallDir = "$env:ProgramFiles\Toluja\PrintAgent",
    [string]$ServiceName = "TolujaPrintAgent",
    [string]$TaskName = "TolujaPrintAgent"
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

$resolvedExe = Resolve-Path -LiteralPath $SourceExe -ErrorAction Stop
$targetExe = Join-Path $InstallDir "toluja-print-agent.exe"
$targetEnv = Join-Path $InstallDir ".env"

New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null
Copy-Item -LiteralPath $resolvedExe -Destination $targetExe -Force

if (-not (Test-Path -LiteralPath $targetEnv)) {
    @(
        "# Configure os valores abaixo e reinicie o serviço/tarefa",
        "API_BASE_URL=http://localhost:8080",
        "DEVICE_ID=agent-store-001",
        "PRINT_KEY=CHANGE_ME",
        "POLL_INTERVAL_MS=1000"
    ) | Set-Content -Path $targetEnv -Encoding UTF8
}

sc.exe query $ServiceName *> $null
if ($LASTEXITCODE -eq 0) {
    sc.exe stop $ServiceName | Out-Null
    Start-Sleep -Seconds 2
    sc.exe delete $ServiceName | Out-Null
    Start-Sleep -Seconds 1
}

if (Get-Command Get-ScheduledTask -ErrorAction SilentlyContinue) {
    $existingTask = Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
    if ($null -ne $existingTask) {
        Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction SilentlyContinue | Out-Null
    }
} else {
    cmd.exe /c "schtasks.exe /Query /TN ""$TaskName"" >nul 2>nul"
    if ($LASTEXITCODE -eq 0) {
        schtasks.exe /Delete /TN $TaskName /F | Out-Null
    }
}

if ($Mode -eq "service") {
    sc.exe create $ServiceName binPath= "\"$targetExe\"" start= auto DisplayName= "\"Toluja Print Agent\"" | Out-Null
    sc.exe description $ServiceName "Toluja local print agent" | Out-Null
    sc.exe failure $ServiceName reset= 86400 actions= restart/5000/restart/5000/restart/5000 | Out-Null
    sc.exe start $ServiceName | Out-Null

    Write-Host "Instalado como servico Windows ($ServiceName)."
    Write-Host "Arquivo de configuracao: $targetEnv"
    Write-Host "Para editar configuracao, altere .env e execute: sc.exe stop $ServiceName; sc.exe start $ServiceName"
    exit 0
}

$taskCommand = '"' + $targetExe + '"'
schtasks.exe /Create /F /TN $TaskName /SC ONSTART /RU SYSTEM /RL HIGHEST /TR $taskCommand | Out-Null
schtasks.exe /Run /TN $TaskName | Out-Null

Write-Host "Instalado como tarefa de inicializacao ($TaskName)."
Write-Host "Arquivo de configuracao: $targetEnv"
Write-Host "Para reiniciar manualmente: schtasks.exe /Run /TN $TaskName"
