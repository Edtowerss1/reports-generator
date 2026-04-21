param(
    [string]$AppName = "JaspertReport",
    [string]$Version = "0.0.1",
    [string]$Vendor = "TuEmpresa",
    [string]$MainClass = "org.springframework.boot.loader.launch.JarLauncher"
)

# Directorio base del proyecto (este script debe estar en carpeta 'scripts')
$ProjectRoot = Split-Path -Parent $PSScriptRoot

Write-Host "====> 1) Construyendo JAR ejecutable con Maven (fat JAR)" -ForegroundColor Cyan
Push-Location $ProjectRoot
try {
    # 1) Construir la aplicación con Maven
    .\mvnw clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "Falló 'mvnw clean package'"
    }

    # 2) Buscar el JAR reempaquetado
    $jarFile = Get-ChildItem "target" -Filter "$AppName-*.jar" | 
        Where-Object { $_.Name -notlike "*.original" } |
        Select-Object -First 1

    if (-not $jarFile) {
        throw "No se encontró el JAR ejecutable en target\"
    }

    Write-Host "JAR encontrado: $($jarFile.Name)" -ForegroundColor Green

    # 3) Preparar directorio temporal para jpackage
    $tempAppInput = Join-Path $ProjectRoot "target\jpackage-input"
    if (Test-Path $tempAppInput) {
        Remove-Item -Recurse -Force $tempAppInput
    }
    New-Item -ItemType Directory -Path $tempAppInput | Out-Null
    
    Copy-Item $jarFile.FullName -Destination $tempAppInput

    # 4) Crear carpeta de salida
    $outputRoot = Join-Path $ProjectRoot "installer-output"
    if (-not (Test-Path $outputRoot)) {
        New-Item -ItemType Directory -Path $outputRoot | Out-Null
    }

    $bundleDirName = "${AppName}-portable-${Version}"
    $tempImageDir = Join-Path $ProjectRoot "target\jpackage-image"
    
    if (Test-Path $tempImageDir) {
        Remove-Item -Recurse -Force $tempImageDir
    }

    Write-Host "====> 2) Generando imagen de aplicación con jpackage (incluye runtime Java)..." -ForegroundColor Cyan

    # Usar jpackage para crear imagen de aplicación (no instalador)
    $jpackageArgs = @(
        "--type", "app-image",
        "--name", $AppName,
        "--app-version", $Version,
        "--vendor", $Vendor,
        "--input", $tempAppInput,
        "--main-jar", $jarFile.Name,
        "--main-class", $MainClass,
        "--dest", $tempImageDir,
        "--win-console",
        "--java-options", "-Xms256m",
        "--java-options", "-Xmx512m"
    )

    Write-Host "Ejecutando: jpackage $($jpackageArgs -join ' ')" -ForegroundColor Gray
    & jpackage @jpackageArgs

    if ($LASTEXITCODE -ne 0) {
        throw "Falló jpackage. Asegúrate de tener JDK 17+ con jpackage instalado."
    }

    # 5) Mover la imagen generada al output final
    $generatedImage = Join-Path $tempImageDir $AppName
    $finalBundleDir = Join-Path $outputRoot $bundleDirName

    if (Test-Path $finalBundleDir) {
        Remove-Item -Recurse -Force $finalBundleDir
    }

    Move-Item $generatedImage $finalBundleDir

    Write-Host "====> Bundle portable creado exitosamente en:" -ForegroundColor Green
    Write-Host "      $finalBundleDir" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Para distribuir:" -ForegroundColor Cyan
    Write-Host "  1. Copia la carpeta completa '$bundleDirName' a la máquina destino" -ForegroundColor White
    Write-Host "  2. Ejecuta '$AppName.exe' dentro de esa carpeta" -ForegroundColor White
    Write-Host ""
    Write-Host "La aplicación incluye su propio Java Runtime, no requiere Java instalado." -ForegroundColor Green

}
finally {
    Pop-Location
}
