@echo off
chcp 65001 >nul
title Instalador Toluja Print Agent

echo ==========================================
echo      Instalando Toluja Print Agent
echo ==========================================
echo.

:: Verifica admin
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo Solicitando permissao de administrador...
    powershell -NoProfile -Command "Start-Process '%~f0' -Verb RunAs"
    exit /b
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0install-windows.ps1"
if %errorlevel% neq 0 (
    echo.
    echo ==========================================
    echo Erro durante a instalacao.
    echo ==========================================
    pause
    exit /b 1
)

echo.
echo ==========================================
echo Instalacao finalizada.
echo ==========================================
pause
