<#
.SYNOPSIS
    Administra los servicios JaspertReport-Instance-A y JaspertReport-Instance-B.

.PARAMETER Action
    Acción a ejecutar: status | start | stop | restart | logs

.PARAMETER Target
    Instancia a administrar: instance-a | instance-b | all (default: all)

.EXAMPLE
    .\scripts\manage-services.ps1 -Action status
    .\scripts\manage-services.ps1 -Action restart -Target instance-a
    .\scripts\manage-services.ps1 -Action stop -Target instance-b
    .\scripts\manage-services.ps1 -Action logs -Target instance-a
#>

param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("status", "start", "stop", "restart", "logs")]
    [string]$Action,

    [ValidateSet("instance-a", "instance-b", "all")]
    [string]$Target = "all"
)

$InstanceAServiceName = "JaspertReport-Instance-A"
$InstanceBServiceName = "JaspertReport-Instance-B"
$InstanceALogsDir     = "C:\servicios\instancia-a\logs"
$InstanceBLogsDir     = "C:\servicios\instancia-b\logs"

function Get-Services {
    if ($Target -eq "instance-a") { return @($InstanceAServiceName) }
    if ($Target -eq "instance-b") { return @($InstanceBServiceName) }
    return @($InstanceAServiceName, $InstanceBServiceName)
}

function Show-Status([string]$name) {
    $svc = Get-Service -Name $name -ErrorAction SilentlyContinue
    if ($svc) {
        $color = if ($svc.Status -eq "Running") { "Green" } else { "Red" }
        Write-Host "  $name : " -NoNewline
        Write-Host "$($svc.Status)" -ForegroundColor $color
    } else {
        Write-Host "  $name : " -NoNewline
        Write-Host "No instalado" -ForegroundColor DarkGray
    }
}

function Show-Logs([string]$name) {
    $logsDir = if ($name -eq $InstanceAServiceName) { $InstanceALogsDir } else { $InstanceBLogsDir }
    $logFile = Join-Path $logsDir "stdout.log"
    if (Test-Path $logFile) {
        Write-Host "  --- Últimas 50 líneas de $logFile ---" -ForegroundColor Cyan
        Get-Content $logFile -Tail 50
    } else {
        Write-Host "  No se encontró log en: $logFile" -ForegroundColor Yellow
    }
}

$services = Get-Services

switch ($Action) {
    "status" {
        Write-Host ""
        Write-Host "  Estado de los servicios:" -ForegroundColor Cyan
        foreach ($svc in $services) { Show-Status $svc }
        Write-Host ""
    }
    "start" {
        foreach ($svc in $services) {
            Write-Host "  Iniciando $svc ..." -ForegroundColor Cyan
            nssm start $svc
        }
    }
    "stop" {
        foreach ($svc in $services) {
            Write-Host "  Deteniendo $svc ..." -ForegroundColor Yellow
            nssm stop $svc
        }
    }
    "restart" {
        foreach ($svc in $services) {
            Write-Host "  Reiniciando $svc ..." -ForegroundColor Cyan
            nssm restart $svc
        }
    }
    "logs" {
        foreach ($svc in $services) {
            Write-Host ""
            Show-Logs $svc
        }
    }
}
