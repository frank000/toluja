$ErrorActionPreference = "Stop"

$AppName = "print-agent"
$OutDir = "dist"

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

Write-Host "Building Windows x64..."
$env:GOOS = "windows"
$env:GOARCH = "amd64"
go build -o "$OutDir/$AppName-windows-amd64.exe" .

Write-Host "Building Linux x64..."
$env:GOOS = "linux"
$env:GOARCH = "amd64"
Remove-Item Env:GOARM -ErrorAction SilentlyContinue
go build -o "$OutDir/$AppName-linux-amd64" .

Write-Host "Building Raspberry Pi (Linux ARM64)..."
$env:GOOS = "linux"
$env:GOARCH = "arm64"
Remove-Item Env:GOARM -ErrorAction SilentlyContinue
go build -o "$OutDir/$AppName-linux-arm64" .

Write-Host "Building Raspberry Pi (Linux ARMv7)..."
$env:GOOS = "linux"
$env:GOARCH = "arm"
$env:GOARM = "7"
go build -o "$OutDir/$AppName-linux-armv7" .

Write-Host "Done. Files in $OutDir/"
