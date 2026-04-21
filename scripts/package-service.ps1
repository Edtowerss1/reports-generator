<#
.SYNOPSIS
    Empaqueta una instancia de servicio en un ZIP listo para distribuir
    a la máquina destino.

.DESCRIPTION
    Genera un archivo ZIP autocontenido que incluye:
      - Bundle portable (JaspertReport.exe + Java Runtime)
      - application.properties de la instancia
      - Plantillas de reportes (.jrxml / .jasper)
      - Script de instalación para ejecutar en la máquina destino

    El ZIP resultante es todo lo que necesitas copiar a la máquina destino.
    No requiere Maven, JDK ni el código fuente en la máquina destino.

.PARAMETER Instance
    Instancia a empaquetar: "instance-a" o "instance-b"

.EXAMPLE
    .\scripts\package-service.ps1 -Instance instance-a
    .\scripts\package-service.ps1 -Instance instance-b
#>

param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("instance-a", "instance-b")]
    [string]$Instance
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ProjectRoot   = Split-Path -Parent $PSScriptRoot
$BundleVersion = "0.0.1"
$BundleName    = "JaspertReport"
$BundleDir     = Join-Path $ProjectRoot "installer-output\${BundleName}-portable-${BundleVersion}"
$DeployDir     = Join-Path $ProjectRoot "deploy\$Instance"
$SrcReportes   = Join-Path $ProjectRoot "src\main\resources\reportes"
$OutputDir     = Join-Path $ProjectRoot "dist"
$PackageName   = "${BundleName}-${Instance}-package"
$TempDir       = Join-Path $ProjectRoot "target\package-temp\$PackageName"
$ZipOutput     = Join-Path $OutputDir "${PackageName}.zip"

$Port        = if ($Instance -eq "instance-a") { 8080 } else { 8081 }
$ServiceName = if ($Instance -eq "instance-a") { "JaspertReport-Instance-A" } else { "JaspertReport-Instance-B" }
$InstallDir  = if ($Instance -eq "instance-a") { "C:\servicios\instancia-a" } else { "C:\servicios\instancia-b" }

function Write-Step([string]$msg) {
    Write-Host "====> $msg" -ForegroundColor Cyan
}
function Write-OK([string]$msg) {
    Write-Host "  [OK] $msg" -ForegroundColor Green
}

# ─────────────────────────────────────────────────────────────────
# VALIDACIONES
# ─────────────────────────────────────────────────────────────────

Write-Step "Validando prerrequisitos..."

if (-not (Test-Path $BundleDir)) {
    Write-Host "  [ERROR] Bundle portable no encontrado: $BundleDir" -ForegroundColor Red
    Write-Host "  Ejecuta primero: .\scripts\build-portable-exe.ps1" -ForegroundColor Yellow
    exit 1
}

$configFile = Join-Path $DeployDir "application.properties"
if (-not (Test-Path $configFile)) {
    Write-Host "  [ERROR] No se encontró: $configFile" -ForegroundColor Red
    exit 1
}

Write-OK "Bundle y configuración encontrados."

# ─────────────────────────────────────────────────────────────────
# PREPARAR ESTRUCTURA TEMPORAL
# ─────────────────────────────────────────────────────────────────

Write-Step "Preparando paquete para instancia '$Instance'..."

if (Test-Path $TempDir) { Remove-Item -Recurse -Force $TempDir }
New-Item -ItemType Directory -Path $TempDir | Out-Null
New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null

# 1. Copiar bundle portable
$TempBundle = Join-Path $TempDir "bundle"
Copy-Item -Recurse "$BundleDir" $TempBundle
Write-OK "Bundle portable copiado."

# 2. Copiar application.properties
Copy-Item $configFile "$TempDir\application.properties"
Write-OK "application.properties copiado."

# 3. Copiar reportes
$TempReportes = Join-Path $TempDir "reportes"
New-Item -ItemType Directory -Path $TempReportes | Out-Null
if (Test-Path $SrcReportes) {
    Copy-Item "$SrcReportes\*" $TempReportes -Force
    Write-OK "Plantillas de reportes copiadas."
} else {
    Write-Host "  [WARN] No se encontró src/main/resources/reportes/" -ForegroundColor Yellow
}

# 4. Generar script de instalación para la máquina destino
$installScript = @"
<#
.SYNOPSIS
    Instala el servicio $ServiceName en esta máquina.

.NOTES
    - Ejecutar como Administrador.
    - Requiere NSSM instalado (https://nssm.cc o choco install nssm -y).
    - Ejecutar desde la carpeta donde se extrajo el ZIP.
#>

#Requires -RunAsAdministrator

Set-StrictMode -Version Latest
`$ErrorActionPreference = "Stop"

`$ServiceName = "$ServiceName"
`$InstallDir  = "$InstallDir"
`$Port        = $Port
`$ScriptDir   = Split-Path -Parent `$MyInvocation.MyCommand.Path

function Write-Step([string]`$msg) { Write-Host "" ; Write-Host "====> `$msg" -ForegroundColor Cyan }
function Write-OK([string]`$msg)   { Write-Host "  [OK] `$msg" -ForegroundColor Green }
function Write-Warn([string]`$msg) { Write-Host "  [WARN] `$msg" -ForegroundColor Yellow }

# Validar NSSM
if (-not (Get-Command nssm -ErrorAction SilentlyContinue)) {
    Write-Host "  [ERROR] NSSM no encontrado. Instálalo con: choco install nssm -y" -ForegroundColor Red
    exit 1
}

Write-Step "Instalando servicio `$ServiceName (puerto `$Port)..."

# Eliminar servicio anterior si existe
`$svc = Get-Service -Name `$ServiceName -ErrorAction SilentlyContinue
if (`$svc) {
    Write-Warn "Servicio anterior encontrado. Eliminando..."
    nssm stop `$ServiceName | Out-Null
    Start-Sleep -Seconds 2
    nssm remove `$ServiceName confirm | Out-Null
    Write-OK "Servicio anterior eliminado."
}

# Crear directorio del servicio
if (Test-Path `$InstallDir) { Remove-Item -Recurse -Force `$InstallDir }
New-Item -ItemType Directory -Path `$InstallDir | Out-Null
Write-OK "Directorio creado: `$InstallDir"

# Copiar bundle
Copy-Item -Recurse "`$ScriptDir\bundle\*" `$InstallDir
Write-OK "Bundle copiado."

# Copiar application.properties
Copy-Item "`$ScriptDir\application.properties" "`$InstallDir\application.properties"
Write-OK "application.properties copiado."

# Copiar reportes
`$reportesDir = "`$InstallDir\reportes"
New-Item -ItemType Directory -Path `$reportesDir -Force | Out-Null
Copy-Item "`$ScriptDir\reportes\*" `$reportesDir -Force
Write-OK "Reportes copiados."

# Crear carpeta de logs
New-Item -ItemType Directory -Path "`$InstallDir\logs" -Force | Out-Null

# Registrar servicio con NSSM
`$exePath = "`$InstallDir\${BundleName}.exe"
nssm install `$ServiceName `$exePath
nssm set `$ServiceName AppDirectory `$InstallDir
nssm set `$ServiceName Description "Motor de reportes JasperReports - `$ServiceName (puerto `$Port)"
nssm set `$ServiceName AppExit Default Restart
nssm set `$ServiceName AppRestartDelay 5000
nssm set `$ServiceName AppStdout "`$InstallDir\logs\stdout.log"
nssm set `$ServiceName AppStderr "`$InstallDir\logs\stderr.log"
nssm set `$ServiceName AppRotateFiles 1
nssm set `$ServiceName AppRotateSeconds 86400
sc.exe config `$ServiceName start= auto | Out-Null
Write-OK "Servicio registrado."

# Iniciar
Write-Step "Iniciando servicio..."
nssm start `$ServiceName
Start-Sleep -Seconds 15

`$status = nssm status `$ServiceName
Write-Host ""
Write-Host "  Estado: `$status" -ForegroundColor $(if (`$status -eq "SERVICE_RUNNING") { "Green" } else { "Red" })
Write-Host ""
Write-Host "  Verificar salud: Invoke-RestMethod http://localhost:`$Port/actuator/health" -ForegroundColor Cyan
Write-Host "  Logs            : `$InstallDir\logs\stdout.log" -ForegroundColor Cyan
"@

$installScript | Set-Content "$TempDir\install.ps1" -Encoding UTF8
Write-OK "Script de instalación generado."

# ─────────────────────────────────────────────────────────────────
# COMPRIMIR
# ─────────────────────────────────────────────────────────────────

Write-Step "Comprimiendo paquete..."

if (Test-Path $ZipOutput) { Remove-Item -Force $ZipOutput }
Compress-Archive -Path "$TempDir\*" -DestinationPath $ZipOutput
Remove-Item -Recurse -Force $TempDir

Write-Host ""
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host "  Paquete generado exitosamente:" -ForegroundColor Green
Write-Host "  $ZipOutput" -ForegroundColor Yellow
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host ""
Write-Host "  Pasos para instalar en la máquina destino:" -ForegroundColor Cyan
Write-Host "  1. Copia el ZIP a la máquina destino" -ForegroundColor White
Write-Host "  2. Extrae el ZIP en cualquier carpeta temporal" -ForegroundColor White
Write-Host "  3. Abre PowerShell como Administrador" -ForegroundColor White
Write-Host "  4. Ejecuta: .\install.ps1" -ForegroundColor White
Write-Host ""
Write-Host "  Requisito en la máquina destino: NSSM instalado." -ForegroundColor Yellow
