param(
    [Parameter(Mandatory = $true)]
    [string]$ManifestPath,

    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path,
    [string]$WinSWVersion = "2.12.0",
    [switch]$SkipInstaller
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $false

function Write-Step([string]$Message) {
    Write-Host "==> $Message"
}

function Assert-Command([string]$Name, [string]$InstallHint) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Comando '$Name' nao encontrado. $InstallHint"
    }
}

function Convert-ToSlug([string]$Value) {
    $normalized = $Value.Trim().ToLowerInvariant()
    $normalized = $normalized -replace '[^a-z0-9\-]+', '-'
    $normalized = $normalized -replace '-+', '-'
    return $normalized.Trim('-')
}

function Write-JsonFile($Object, [string]$Path) {
    $Object | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $Path -Encoding UTF8
}

$manifestFullPath = (Resolve-Path -LiteralPath $ManifestPath).Path
$manifest = Get-Content -LiteralPath $manifestFullPath -Raw | ConvertFrom-Json

$clientSlug = Convert-ToSlug $manifest.clientSlug
$storeSlug = Convert-ToSlug $manifest.storeSlug
$agentVersion = if ($manifest.agentVersion) { [string]$manifest.agentVersion } else { "0.1.0-dev" }

if ([string]::IsNullOrWhiteSpace($clientSlug) -or [string]::IsNullOrWhiteSpace($storeSlug)) {
    throw "Manifest precisa informar clientSlug e storeSlug."
}
if ($null -eq $manifest.config) {
    throw "Manifest precisa conter objeto config."
}

$buildRoot = Join-Path $ProjectRoot "build\windows-package"
$stageDir = Join-Path $buildRoot "stage"
$appDir = Join-Path $stageDir "app"
$runtimeDir = Join-Path $stageDir "runtime"
$serviceDir = Join-Path $stageDir "service"
$configDir = Join-Path $stageDir "config"
$downloadDir = Join-Path $buildRoot "downloads"
$installerOut = Join-Path $buildRoot "installer"

Write-Step "Limpando staging..."
Remove-Item -LiteralPath $stageDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $appDir, $runtimeDir, $serviceDir, $configDir, $downloadDir, $installerOut | Out-Null

Write-Step "Compilando JAR..."
$gradlew = Join-Path $ProjectRoot "gradlew.bat"
if (-not (Test-Path -LiteralPath $gradlew)) {
    throw "Gradle Wrapper nao encontrado: $gradlew"
}
& $gradlew "-p" $ProjectRoot ":app:jar" "-PagentVersion=$agentVersion"
if ($LASTEXITCODE -ne 0) { throw "Falha no build Gradle." }
$jar = Get-ChildItem -LiteralPath (Join-Path $ProjectRoot "app\build\libs") -Filter "toluja-print-agent*.jar" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if ($null -eq $jar) { throw "JAR nao encontrado em app\build\libs." }
Copy-Item -LiteralPath $jar.FullName -Destination (Join-Path $appDir "toluja-print-agent.jar") -Force

Write-Step "Gerando runtime reduzido com jlink..."
Assert-Command "jlink" "Instale JDK 21."
& jlink `
    "--add-modules" "java.base,java.desktop,java.logging,java.net.http" `
    "--strip-debug" `
    "--no-header-files" `
    "--no-man-pages" `
    "--compress=zip-6" `
    "--output" $runtimeDir
if ($LASTEXITCODE -ne 0) { throw "Falha ao gerar runtime com jlink." }

Write-Step "Gerando config.json do cliente..."
Write-JsonFile $manifest.config (Join-Path $configDir "config.json")

Write-Step "Baixando WinSW $WinSWVersion..."
$winswExe = Join-Path $downloadDir "WinSW-x64.exe"
if (-not (Test-Path -LiteralPath $winswExe)) {
    $winswUrl = "https://github.com/winsw/winsw/releases/download/v$WinSWVersion/WinSW-x64.exe"
    Invoke-WebRequest -Uri $winswUrl -OutFile $winswExe
}
Copy-Item -LiteralPath $winswExe -Destination (Join-Path $serviceDir "TolujaPrintAgentService.exe") -Force
Copy-Item -LiteralPath (Join-Path $ProjectRoot "packaging\windows\winsw\TolujaPrintAgentService.xml") `
    -Destination (Join-Path $serviceDir "TolujaPrintAgentService.xml") -Force

if ($SkipInstaller) {
    Write-Host "Staging pronto em: $stageDir"
    exit 0
}

Write-Step "Gerando instalador Inno Setup..."
Assert-Command "iscc" "Instale Inno Setup e garanta que ISCC.exe esteja no PATH."
$iss = Join-Path $ProjectRoot "packaging\windows\installer\toluja-print-agent.iss"
& iscc `
    "/DAppVersion=$agentVersion" `
    "/DClientSlug=$clientSlug" `
    "/DStoreSlug=$storeSlug" `
    "/DStageDir=$stageDir" `
    "/DOutputDir=$installerOut" `
    $iss
if ($LASTEXITCODE -ne 0) { throw "Falha ao gerar instalador." }

Write-Host ""
Write-Host "Pacote Windows gerado:"
Get-ChildItem -LiteralPath $installerOut -Filter "*.exe" | ForEach-Object { Write-Host $_.FullName }
