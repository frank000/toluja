#!/usr/bin/env bash
set -euo pipefail

APP_NAME="print-agent"
OUT_DIR="dist"

mkdir -p "${OUT_DIR}"

echo "Building Windows x64..."
GOOS=windows GOARCH=amd64 go build -o "${OUT_DIR}/${APP_NAME}-windows-amd64.exe" .

echo "Building Linux x64..."
GOOS=linux GOARCH=amd64 go build -o "${OUT_DIR}/${APP_NAME}-linux-amd64" .

echo "Building Raspberry Pi (Linux ARM64)..."
GOOS=linux GOARCH=arm64 go build -o "${OUT_DIR}/${APP_NAME}-linux-arm64" .

echo "Building Raspberry Pi (Linux ARMv7)..."
GOOS=linux GOARCH=arm GOARM=7 go build -o "${OUT_DIR}/${APP_NAME}-linux-armv7" .

echo "Done. Files in ${OUT_DIR}/"
