# Instrucciones para Distribución de JaspertReport

## ✅ Bundle Portable Generado Exitosamente

Se ha creado un bundle portable completo en:
```
installer-output\JaspertReport-portable-0.0.1\
```

## 📦 Contenido del Bundle

El bundle incluye:
- ✅ **JaspertReport.exe** - Ejecutable principal (447 KB)
- ✅ **runtime/** - Java Runtime embebido completo (~150 MB)
- ✅ **app/** - Tu aplicación Spring Boot con todas las dependencias
- ✅ **JaspertReport.ico** - Ícono de la aplicación
- ✅ **README.txt** - Instrucciones para el usuario final

## 🚀 Cómo Distribuir

### Opción 1: Carpeta Comprimida (Recomendado)
1. Comprime la carpeta `JaspertReport-portable-0.0.1` en un archivo ZIP:
   ```powershell
   Compress-Archive -Path .\installer-output\JaspertReport-portable-0.0.1 -DestinationPath .\JaspertReport-v0.0.1.zip
   ```

2. Distribuye el archivo ZIP a tus usuarios

3. Los usuarios deben:
   - Descomprimir el ZIP en cualquier ubicación
   - Ejecutar `JaspertReport.exe`

### Opción 2: Carpeta Directa
Copia toda la carpeta `JaspertReport-portable-0.0.1` a una unidad USB o servidor de archivos.

## 💻 En la Máquina Destino

### ✅ Ventajas
- ✅ **NO requiere instalar Java** - El runtime está incluido
- ✅ **NO requiere instalación** - Es completamente portable
- ✅ **NO requiere permisos de administrador** - Se puede ejecutar desde cualquier carpeta
- ✅ **NO modifica el registro de Windows**
- ✅ **Fácil de desinstalar** - Solo elimina la carpeta

### 📋 Requisitos Mínimos
- Windows 10 o superior (64-bit)
- 512 MB de RAM disponible
- ~200 MB de espacio en disco

### 🎯 Ejecución
Los usuarios simplemente deben:
1. Descomprimir/copiar la carpeta a su ubicación deseada
2. Doble clic en `JaspertReport.exe`
3. La aplicación inicia en `http://localhost:8080`

## 🔧 Regenerar el Bundle

Si necesitas regenerar el bundle portable (por ejemplo, después de cambios en el código):

```powershell
# Desde la raíz del proyecto
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\build-portable-exe.ps1
```

El script hace automáticamente:
1. Compila el proyecto con Maven (`mvnw clean package`)
2. Genera el JAR ejecutable
3. Usa `jpackage` para crear la imagen de aplicación con runtime embebido
4. Copia todo a `installer-output\JaspertReport-portable-0.0.1\`

## 🎨 Personalización

Para personalizar el bundle, edita los parámetros en `scripts\build-portable-exe.ps1`:

```powershell
param(
    [string]$AppName = "JaspertReport",       # Nombre de la aplicación
    [string]$Version = "0.0.1",               # Versión
    [string]$Vendor = "TuEmpresa"             # Tu empresa
)
```

## 📊 Tamaño Aproximado del Bundle

- Runtime Java: ~150 MB
- Aplicación + dependencias: ~50 MB
- **Total: ~200 MB** (comprimido: ~80 MB)

## 🔒 Seguridad

Algunos antivirus pueden marcar el ejecutable como sospechoso la primera vez. Esto es normal para aplicaciones Java empaquetadas. Los usuarios pueden:
- Agregar una excepción en el antivirus
- Ejecutar como administrador si es necesario
- Verificar el hash del archivo para confirmar integridad

## ✨ Listo para Producción

Tu aplicación está lista para distribuir. El bundle portable incluye todo lo necesario y funcionará en cualquier máquina Windows sin configuración adicional.
