$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $false

$ServiceName = "TolujaPrintAgent"
$DisplayName = "Toluja Print Agent"
$Description = "Agente local de impressao do Toluja"
$InstallDir = "C:\Program Files\Toluja\PrintAgent"
$LogsDir = Join-Path $InstallDir "logs"
$EnvTarget = Join-Path $InstallDir ".env"
$ExeTarget = Join-Path $InstallDir "toluja-print-agent.exe"

function Write-Step($msg) {
    Write-Host "==> $msg"
}

function Test-IsAdmin {
    $currentUser = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
    return $currentUser.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Get-ExeSource {
    $preferred = Join-Path $PSScriptRoot "toluja-print-agent.exe"
    if (Test-Path $preferred) {
        return $preferred
    }

    $legacy = Join-Path $PSScriptRoot "print-agent-windows-amd64.exe"
    if (Test-Path $legacy) {
        return $legacy
    }

    throw "Executavel nao encontrado. Coloque toluja-print-agent.exe ou print-agent-windows-amd64.exe ao lado deste script."
}

function Get-EnvSource {
    $envPath = Join-Path $PSScriptRoot ".env"
    if (Test-Path $envPath) {
        return $envPath
    }

    $envExamplePath = Join-Path $PSScriptRoot ".env.example"
    if (Test-Path $envExamplePath) {
        return $envExamplePath
    }

    throw "Arquivo .env nao encontrado. Coloque um .env ao lado do instalador."
}

function Stop-And-Delete-ServiceIfExists {
    param(
        [string]$Name
    )

    $service = Get-Service -Name $Name -ErrorAction SilentlyContinue
    if ($null -eq $service) {
        Write-Step "Servico antigo nao existe."
        return
    }

    Write-Step "Parando servico antigo..."
    try {
        if ($service.Status -ne 'Stopped') {
            Stop-Service -Name $Name -Force -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 2
        }
    } catch {
        Write-Host "Aviso: nao foi possivel parar o servico agora."
    }

    Write-Step "Removendo servico antigo..."
    sc.exe delete $Name | Out-Null

    Write-Step "Aguardando remocao completa do servico..."
    for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Seconds 1
        $check = Get-Service -Name $Name -ErrorAction SilentlyContinue
        if ($null -eq $check) {
            Write-Host "Servico removido."
            return
        }
    }

    throw "O servico '$Name' ainda esta marcado para exclusao. Feche services.msc / PowerShell e tente novamente."
}

try {
    if (-not (Test-IsAdmin)) {
        throw "Este script precisa ser executado como Administrador."
    }

    $ExeSource = Get-ExeSource
    $EnvSource = Get-EnvSource

    Write-Step "Validando arquivos..."
    if (!(Test-Path $ExeSource)) {
        throw "Arquivo nao encontrado: $ExeSource"
    }
    if (!(Test-Path $EnvSource)) {
        throw "Arquivo nao encontrado: $EnvSource"
    }

    Write-Step "Criando diretorios..."
    New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
    New-Item -ItemType Directory -Force -Path $LogsDir | Out-Null

    Write-Step "Copiando executavel..."
    Copy-Item -Path $ExeSource -Destination $ExeTarget -Force

    Write-Step "Copiando .env..."
    Copy-Item -Path $EnvSource -Destination $EnvTarget -Force

    Stop-And-Delete-ServiceIfExists -Name $ServiceName

    Write-Step "Criando servico..."
    sc.exe create $ServiceName binPath= "\"$ExeTarget\"" start= auto DisplayName= "\"$DisplayName\"" | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao criar o servico."
    }

    Write-Step "Definindo descricao..."
    sc.exe description $ServiceName $Description | Out-Null

    Write-Step "Configurando reinicio automatico..."
    sc.exe failure $ServiceName reset= 86400 actions= restart/5000/restart/5000/restart/5000 | Out-Null

    Write-Step "Garantindo start automatico..."
    sc.exe config $ServiceName start= auto | Out-Null

    Write-Step "Iniciando servico..."
    Start-Service -Name $ServiceName

    Write-Step "Validando status..."
    $svc = Get-Service -Name $ServiceName -ErrorAction Stop
    Write-Host ""
    Write-Host "Instalacao concluida com sucesso."
    Write-Host "Servico: $($svc.Name)"
    Write-Host "Status : $($svc.Status)"
    Write-Host "Pasta  : $InstallDir"
    Write-Host "Env    : $EnvTarget"
}
catch {
    Write-Host ""
    Write-Host "ERRO NA INSTALACAO:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
