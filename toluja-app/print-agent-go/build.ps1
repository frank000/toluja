$ErrorActionPreference = "Stop"

$AppName = "print-agent"
$OutDir = "dist"
$Version = if ($env:VERSION) { $env:VERSION } else { "dev" }
$Commit = if ($env:GIT_COMMIT) { $env:GIT_COMMIT } else { "none" }
$BuildTime = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
$Ldflags = "-s -w -X main.version=$Version -X main.commit=$Commit -X main.buildTime=$BuildTime"

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

Write-Host "Building Windows x64..."
$env:GOOS = "windows"
$env:GOARCH = "amd64"
go build -trimpath -ldflags $Ldflags -o "$OutDir/$AppName-windows-amd64.exe" .

Write-Host "Building Linux x64..."
$env:GOOS = "linux"
$env:GOARCH = "amd64"
Remove-Item Env:GOARM -ErrorAction SilentlyContinue
go build -trimpath -ldflags $Ldflags -o "$OutDir/$AppName-linux-amd64" .

Write-Host "Building Raspberry Pi (Linux ARM64)..."
$env:GOOS = "linux"
$env:GOARCH = "arm64"
Remove-Item Env:GOARM -ErrorAction SilentlyContinue
go build -trimpath -ldflags $Ldflags -o "$OutDir/$AppName-linux-arm64" .

Write-Host "Building Raspberry Pi (Linux ARMv7)..."
$env:GOOS = "linux"
$env:GOARCH = "arm"
$env:GOARM = "7"
go build -trimpath -ldflags $Ldflags -o "$OutDir/$AppName-linux-armv7" .

Remove-Item Env:GOOS -ErrorAction SilentlyContinue
Remove-Item Env:GOARCH -ErrorAction SilentlyContinue
Remove-Item Env:GOARM -ErrorAction SilentlyContinue

Write-Host "Done. Files in $OutDir/"
