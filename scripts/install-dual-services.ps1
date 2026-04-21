<#
.SYNOPSIS
    Instala dos instancias de JaspertReport como servicios Windows:
    una para Instance A (puerto 8080) y otra para Instance B (puerto 8081).

.DESCRIPTION
    Este script realiza las siguientes acciones:
      1. Valida que NSSM esté disponible y que el bundle portable exista.
      2. Crea los directorios de servicio en C:\servicios\instancia-a\ y C:\servicios\instancia-b\.
      3. Copia el bundle portable a cada directorio de servicio.
      4. Copia el application.properties correspondiente a cada servicio.
      5. Modifica el JaspertReport.cfg de cada instancia para apuntar a su config.
      6. Crea la carpeta de reportes de cada instancia y copia los .jrxml del proyecto.
      7. Registra y arranca ambos servicios con NSSM.

.NOTES
    - Ejecutar como Administrador.
    - Requiere NSSM instalado (choco install nssm -y).
    - Requiere que el bundle portable esté generado en installer-output/.
      Si aún no fue generado, ejecuta primero: .\scripts\build-portable-exe.ps1

.EXAMPLE
    # Desde la raíz del proyecto, como Administrador:
    .\scripts\install-dual-services.ps1
#>

#Requires -RunAsAdministrator

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ─────────────────────────────────────────────────────────────────
# CONFIGURACIÓN — Ajusta estas rutas si es necesario
# ─────────────────────────────────────────────────────────────────
$ProjectRoot     = Split-Path -Parent $PSScriptRoot
$BundleVersion   = "0.0.1"
$BundleName      = "JaspertReport"
$BundleDir       = Join-Path $ProjectRoot "installer-output\${BundleName}-portable-${BundleVersion}"
$DeployDir       = Join-Path $ProjectRoot "deploy"

# Directorios destino de cada servicio en el servidor
$InstanceAServiceDir = "C:\servicios\instancia-a"
$InstanceBServiceDir = "C:\servicios\instancia-b"

# Nombres de los servicios Windows
$InstanceAServiceName = "JaspertReport-Instance-A"
$InstanceBServiceName = "JaspertReport-Instance-B"

# ─────────────────────────────────────────────────────────────────
# FUNCIONES AUXILIARES
# ─────────────────────────────────────────────────────────────────

function Write-Step([string]$msg) {
    Write-Host ""
    Write-Host "====> $msg" -ForegroundColor Cyan
}

function Write-OK([string]$msg) {
    Write-Host "  [OK] $msg" -ForegroundColor Green
}

function Write-Warn([string]$msg) {
    Write-Host "  [WARN] $msg" -ForegroundColor Yellow
}

function Stop-And-Remove-Service([string]$name) {
    $svc = Get-Service -Name $name -ErrorAction SilentlyContinue
    if ($svc) {
        Write-Warn "El servicio '$name' ya existe. Deteniéndolo y eliminándolo..."
        nssm stop $name | Out-Null
        Start-Sleep -Seconds 2
        nssm remove $name confirm | Out-Null
        Write-OK "Servicio '$name' eliminado."
    }
}

function Install-Instance {
    param(
        [string]$ServiceName,
        [string]$ServiceDir,
        [string]$ConfigSource,   # ruta al application.properties de deploy/
        [int]   $Port
    )

    Write-Step "Configurando instancia: $ServiceName (puerto $Port)"

    # 1. Crear directorio del servicio
    if (Test-Path $ServiceDir) {
        Write-Warn "El directorio '$ServiceDir' ya existe. Se sobreescribirá el bundle."
        Remove-Item -Recurse -Force $ServiceDir
    }
    New-Item -ItemType Directory -Path $ServiceDir | Out-Null
    Write-OK "Directorio creado: $ServiceDir"

    # 2. Copiar bundle portable completo
    Copy-Item -Recurse -Force "$BundleDir\*" $ServiceDir
    Write-OK "Bundle portable copiado."

    # 3. Copiar application.properties al directorio del servicio
    Copy-Item -Force $ConfigSource "$ServiceDir\application.properties"
    Write-OK "application.properties copiado."

    # 4. Spring Boot encuentra application.properties automáticamente porque
    #    NSSM establece AppDirectory = $ServiceDir como directorio de trabajo,
    #    y el archivo ya fue copiado ahí en el paso anterior.
    #    No es necesario modificar JaspertReport.cfg.

    # 5. Crear carpeta de reportes y copiar plantillas del proyecto
    $reportesDir = "$ServiceDir\reportes"
    New-Item -ItemType Directory -Path $reportesDir -Force | Out-Null

    $srcReportes = Join-Path $ProjectRoot "src\main\resources\reportes"
    if (Test-Path $srcReportes) {
        Copy-Item -Path "$srcReportes\*" -Destination $reportesDir -Force
        Write-OK "Plantillas de reportes copiadas a: $reportesDir"
    } else {
        Write-Warn "No se encontró src/main/resources/reportes/. Crea la carpeta '$reportesDir' y agrega los .jrxml manualmente."
    }

    # 6. Eliminar servicio anterior si existe
    Stop-And-Remove-Service -name $ServiceName

    # 7. Instalar servicio NSSM
    $exePath = Join-Path $ServiceDir "${BundleName}.exe"
    nssm install $ServiceName $exePath
    nssm set $ServiceName AppDirectory $ServiceDir
    nssm set $ServiceName Description "Motor de reportes JasperReports - $ServiceName (puerto $Port)"
    nssm set $ServiceName AppExit Default Restart
    nssm set $ServiceName AppRestartDelay 5000
    nssm set $ServiceName AppStdout "$ServiceDir\logs\stdout.log"
    nssm set $ServiceName AppStderr "$ServiceDir\logs\stderr.log"
    nssm set $ServiceName AppRotateFiles 1
    nssm set $ServiceName AppRotateSeconds 86400

    # Crear carpeta de logs
    New-Item -ItemType Directory -Path "$ServiceDir\logs" -Force | Out-Null

    # Inicio automático
    sc.exe config $ServiceName start= auto | Out-Null

    Write-OK "Servicio '$ServiceName' registrado correctamente."
}

# ─────────────────────────────────────────────────────────────────
# VALIDACIONES PREVIAS
# ─────────────────────────────────────────────────────────────────

Write-Step "Validando prerrequisitos..."

# Verificar NSSM
if (-not (Get-Command nssm -ErrorAction SilentlyContinue)) {
    Write-Host ""
    Write-Host "  [ERROR] NSSM no encontrado en el PATH." -ForegroundColor Red
    Write-Host "  Instálalo con: choco install nssm -y" -ForegroundColor Yellow
    Write-Host "  O descárgalo de https://nssm.cc/download y agrega nssm.exe al PATH." -ForegroundColor Yellow
    exit 1
}
Write-OK "NSSM disponible."

# Verificar que el bundle portable exista
if (-not (Test-Path $BundleDir)) {
    Write-Host ""
    Write-Host "  [ERROR] Bundle portable no encontrado en: $BundleDir" -ForegroundColor Red
    Write-Host "  Genera el bundle primero ejecutando:" -ForegroundColor Yellow
    Write-Host "    .\scripts\build-portable-exe.ps1" -ForegroundColor Yellow
    exit 1
}

$exeCheck = Join-Path $BundleDir "${BundleName}.exe"
if (-not (Test-Path $exeCheck)) {
    Write-Host "  [ERROR] No se encontró ${BundleName}.exe en el bundle." -ForegroundColor Red
    exit 1
}
Write-OK "Bundle portable encontrado: $BundleDir"

# Verificar configs de deploy
$instanceAConfig = Join-Path $DeployDir "instance-a\application.properties"
$instanceBConfig = Join-Path $DeployDir "instance-b\application.properties"

if (-not (Test-Path $instanceAConfig)) {
    Write-Host "  [ERROR] No se encontró: $instanceAConfig" -ForegroundColor Red
    exit 1
}
if (-not (Test-Path $instanceBConfig)) {
    Write-Host "  [ERROR] No se encontró: $instanceBConfig" -ForegroundColor Red
    exit 1
}
Write-OK "Archivos de configuración encontrados."

# ─────────────────────────────────────────────────────────────────
# INSTALACIÓN DE AMBAS INSTANCIAS
# ─────────────────────────────────────────────────────────────────

Install-Instance `
    -ServiceName $InstanceAServiceName `
    -ServiceDir  $InstanceAServiceDir `
    -ConfigSource $instanceAConfig `
    -Port 8080

Install-Instance `
    -ServiceName $InstanceBServiceName `
    -ServiceDir  $InstanceBServiceDir `
    -ConfigSource $instanceBConfig `
    -Port 8081

# ─────────────────────────────────────────────────────────────────
# INICIO DE SERVICIOS
# ─────────────────────────────────────────────────────────────────

Write-Step "Iniciando servicios..."

nssm start $InstanceAServiceName
Start-Sleep -Seconds 3
nssm start $InstanceBServiceName

Write-Host ""
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host "  Instalación completada exitosamente." -ForegroundColor Green
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host ""
Write-Host "  Instancia A:" -ForegroundColor White
Write-Host "    Servicio : $InstanceAServiceName" -ForegroundColor Gray
Write-Host "    URL      : http://localhost:8080" -ForegroundColor Gray
Write-Host "    Directorio: $InstanceAServiceDir" -ForegroundColor Gray
Write-Host "    Logs     : $InstanceAServiceDir\logs\" -ForegroundColor Gray
Write-Host ""
Write-Host "  Instancia B:" -ForegroundColor White
Write-Host "    Servicio : $InstanceBServiceName" -ForegroundColor Gray
Write-Host "    URL      : http://localhost:8081" -ForegroundColor Gray
Write-Host "    Directorio: $InstanceBServiceDir" -ForegroundColor Gray
Write-Host "    Logs     : $InstanceBServiceDir\logs\" -ForegroundColor Gray
Write-Host ""
Write-Host "  Para verificar el estado:" -ForegroundColor Cyan
Write-Host "    nssm status $InstanceAServiceName" -ForegroundColor White
Write-Host "    nssm status $InstanceBServiceName" -ForegroundColor White
Write-Host ""
Write-Host "  Para administrar los servicios: services.msc" -ForegroundColor Cyan
