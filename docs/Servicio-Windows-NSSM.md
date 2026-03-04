# Ejecutar JaspertReport como servicio de Windows (NSSM)

Esta es la opción recomendada para este proyecto:

- Usar el ejecutable portable generado con `jpackage`.
- Registrarlo como servicio con `NSSM`.

Con esto, la API arranca en segundo plano al iniciar Windows y se administra desde `services.msc`.

## 1) Prerrequisitos

- Tener generado el bundle portable en:
  - `installer-output/JaspertReport-portable-0.0.1/`
- Verificar que exista:
  - `installer-output/JaspertReport-portable-0.0.1/JaspertReport.exe`
- Ejecutar consola PowerShell como Administrador.

## 2) Instalar NSSM

Con Chocolatey:

```powershell
choco install nssm -y
```

Si no usas Chocolatey, descarga NSSM y deja `nssm.exe` en una ruta del `PATH`.

## 3) Crear el servicio

```powershell
nssm install JaspertReportAPI "C:\Proyectos\JaspertReport\installer-output\JaspertReport-portable-0.0.1\JaspertReport.exe"
```

Configurar directorio de trabajo:

```powershell
nssm set JaspertReportAPI AppDirectory "C:\Proyectos\JaspertReport\installer-output\JaspertReport-portable-0.0.1"
```

Configurar inicio automático:

```powershell
sc.exe config JaspertReportAPI start= auto
```

## 4) Iniciar y verificar

Iniciar servicio:

```powershell
nssm start JaspertReportAPI
```

Ver estado:

```powershell
sc.exe query JaspertReportAPI
```

Abrir administrador de servicios:

```powershell
services.msc
```

## 5) Reinicio automático ante fallos (recomendado)

```powershell
nssm set JaspertReportAPI AppExit Default Restart
```

## 6) Detener, reiniciar y desinstalar

Detener:

```powershell
nssm stop JaspertReportAPI
```

Reiniciar:

```powershell
nssm restart JaspertReportAPI
```

Desinstalar:

```powershell
nssm stop JaspertReportAPI
nssm remove JaspertReportAPI confirm
```

## Notas importantes para este proyecto

- Esta opción **sí lo deja como servicio de Windows**, aunque internamente ejecute un `.exe` Java empaquetado.
- No requiere Java instalado aparte en el servidor destino, porque el bundle portable ya incluye runtime.
- Si cambias versión del bundle (`0.0.1` a otra), actualiza la ruta del ejecutable en el servicio.
