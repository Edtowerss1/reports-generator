<#
.SYNOPSIS
    Compila el proyecto, genera el bundle portable e instala las dos instancias
    como servicios Windows en un solo paso.

.DESCRIPTION
    Este script ejecuta en orden:
      1. mvnw clean package  → genera el JAR ejecutable
      2. build-portable-exe.ps1 → empaqueta con jpackage (incluye Java runtime)
      3. install-dual-services.ps1 → despliega Instance A (8080) y Instance B (8081) como servicios

.NOTES
    - Ejecutar como Administrador.
    - Requiere JDK 17+ con jpackage y Maven Wrapper disponibles.
    - Requiere NSSM instalado (choco install nssm -y).

.EXAMPLE
    # Desde la raíz del proyecto, como Administrador:
    .\scripts\build-and-deploy.ps1
#>

#Requires -RunAsAdministrator

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot

function Write-Step([string]$msg) {
    Write-Host ""
    Write-Host "══════════════════════════════════════════════════" -ForegroundColor DarkCyan
    Write-Host "  $msg" -ForegroundColor Cyan
    Write-Host "══════════════════════════════════════════════════" -ForegroundColor DarkCyan
}

# ─────────────────────────────────────────────────────────────────
# PASO 1: Build del bundle portable
# ─────────────────────────────────────────────────────────────────

Write-Step "PASO 1/2 — Compilando y generando bundle portable..."

$buildScript = Join-Path $PSScriptRoot "build-portable-exe.ps1"
& $buildScript

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Falló la generación del bundle portable." -ForegroundColor Red
    exit 1
}

# ─────────────────────────────────────────────────────────────────
# PASO 2: Instalación de los dos servicios
# ─────────────────────────────────────────────────────────────────

Write-Step "PASO 2/2 — Instalando servicios Windows (Instance A + Instance B)..."

$installScript = Join-Path $PSScriptRoot "install-dual-services.ps1"
& $installScript

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Falló la instalación de los servicios." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "  Build y despliegue completados." -ForegroundColor Green
