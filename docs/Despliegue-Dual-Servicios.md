# Despliegue Dual de Servicios — Laboratorio y Gases

Este documento describe el proceso completo para desplegar dos instancias independientes del motor de reportes JaspertReport como servicios Windows, una para la empresa de **Laboratorio** y otra para la empresa de **Gases**.

---

## Arquitectura resultante

```
JaspertReport.jar (único binario)
    │
    ├─ Servicio: JaspertReport-Lab   → Puerto 8080
    │     C:\servicios\lab\
    │     ├── JaspertReport.exe
    │     ├── application.properties   ← BD labclibajaire
    │     ├── reportes\                ← plantillas .jrxml del laboratorio
    │     └── logs\
    │
    └─ Servicio: JaspertReport-Gases  → Puerto 8081
          C:\servicios\gases\
          ├── JaspertReport.exe
          ├── application.properties   ← BD gases
          ├── reportes\                ← plantillas .jrxml de gases
          └── logs\
```

Cada instancia:
- Corre en su propio puerto
- Conecta **exclusivamente** a su propia base de datos
- Tiene su propio token de autenticación
- Tiene su propia carpeta de plantillas de reportes
- Genera sus propios logs

---

## Prerrequisitos

| Requisito | Verificación |
|---|---|
| JDK 17+ con `jpackage` | `jpackage --version` |
| Maven Wrapper en el proyecto | `.\mvnw --version` |
| NSSM instalado | `nssm version` |
| PowerShell como Administrador | Para instalar servicios Windows |

### Instalar NSSM (si no está instalado)

**Opción A — Chocolatey:**
```powershell
choco install nssm -y
```

**Opción B — Manual:**
1. Descargar desde https://nssm.cc/download → versión `nssm 2.24`
2. Copiar `nssm-2.24\win64\nssm.exe` a `C:\Windows\System32\`
3. Verificar: `nssm version`

---

## Estructura de archivos del proyecto

Los archivos relevantes para este despliegue son:

```
proyecto/
├── deploy/
│   ├── lab/
│   │   └── application.properties     ← config instancia Laboratorio
│   └── gases/
│       └── application.properties     ← config instancia Gases
└── scripts/
    ├── build-portable-exe.ps1          ← genera el bundle portable
    ├── install-dual-services.ps1       ← instala ambos servicios
    ├── build-and-deploy.ps1            ← build + install en un paso
    └── manage-services.ps1             ← gestión diaria
```

---

## Configuración de cada instancia

### `deploy/lab/application.properties`

```properties
spring.application.name=JaspertReport-Lab
server.port=8080
server.address=0.0.0.0

service.token=java-service-lab-2026
app.reportes.ruta=C:/servicios/lab/reportes/

spring.datasource.url=jdbc:mysql://HOST:PORT/labclibajaire?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=CONVERT_TO_NULL
spring.datasource.username=USUARIO
spring.datasource.password=CONTRASEÑA
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
```

### `deploy/gases/application.properties`

```properties
spring.application.name=JaspertReport-Gases
server.port=8081
server.address=0.0.0.0

service.token=java-service-gases-2026
app.reportes.ruta=C:/servicios/gases/reportes/

spring.datasource.url=jdbc:mysql://HOST:PORT/gases?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=CONVERT_TO_NULL
spring.datasource.username=USUARIO
spring.datasource.password=CONTRASEÑA
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
```

> **Importante:** Ajusta los valores de `HOST`, `PORT`, `USUARIO` y `CONTRASEÑA` según el entorno antes de desplegar.

---

## Pasos de despliegue (primera vez)

### Paso 1 — Compilar el proyecto

Desde la raíz del proyecto:

```powershell
.\mvnw clean package -DskipTests
```

Resultado esperado: `BUILD SUCCESS` y archivo `target\JaspertReport-*.jar` generado.

---

### Paso 2 — Generar el bundle portable

```powershell
.\scripts\build-portable-exe.ps1
```

Este script:
- Ejecuta `mvnw clean package` si no se hizo antes
- Empaqueta la aplicación con `jpackage` incluyendo el Java Runtime
- Genera el bundle en `installer-output\JaspertReport-portable-0.0.1\`

Resultado esperado:
```
Bundle portable creado exitosamente en:
      installer-output\JaspertReport-portable-0.0.1
```

---

### Paso 3 — Instalar los servicios Windows

> Ejecutar PowerShell **como Administrador**.

```powershell
.\scripts\install-dual-services.ps1
```

Este script realiza por cada instancia (Lab y Gases):
1. Crea el directorio del servicio (`C:\servicios\lab\` y `C:\servicios\gases\`)
2. Copia el bundle portable completo
3. Copia el `application.properties` correspondiente al directorio raíz del servicio
4. Copia las plantillas `.jrxml` a la carpeta `reportes\`
5. Registra el servicio con NSSM con inicio automático y rotación de logs
6. Inicia ambos servicios

Resultado esperado:
```
[OK] Servicio 'JaspertReport-Lab' registrado correctamente.
[OK] Servicio 'JaspertReport-Gases' registrado correctamente.
```

---

### Paso 4 — Verificar que los servicios levantaron

Esperar ~15 segundos para que Spring Boot arranque completamente:

```powershell
Start-Sleep -Seconds 15
nssm status JaspertReport-Lab
nssm status JaspertReport-Gases
```

Ambos deben mostrar: `SERVICE_RUNNING`

---

### Paso 5 — Verificar salud de cada instancia

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"
Invoke-RestMethod -Uri "http://localhost:8081/actuator/health"
```

Respuesta esperada de cada uno:
```json
{ "status": "UP" }
```

---

### Paso 6 — Verificar en los logs el puerto y BD

```powershell
Get-Content C:\servicios\lab\logs\stdout.log -Tail 20
Get-Content C:\servicios\gases\logs\stdout.log -Tail 20
```

En el log de cada servicio deberías ver:
```
Tomcat started on port(s): 8080   ← en lab
Tomcat started on port(s): 8081   ← en gases
Started JaspertReportApplication in X.XXX seconds
```

---

### Paso 7 — Probar cada endpoint

**Laboratorio** (`POST /reportes/generar`):
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/reportes/generar" `
  -Method POST `
  -Headers @{ "X-Service-Token" = "java-service-lab-2026" } `
  -ContentType "application/json" `
  -Body '{
    "reportName": "Laboratorio",
    "format": "PDF",
    "queries": [
      { "param": "DS_EMPRESA", "query": "SELECT 1 AS test" }
    ]
  }'
```

**Gases** (`POST /reportes/imprimir`):
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/reportes/imprimir" `
  -Method POST `
  -Headers @{ "X-Service-Token" = "java-service-gases-2026" } `
  -ContentType "application/json" `
  -Body '{
    "reportName": "StickerQR",
    "printerName": "EPSON TM-T20II",
    "copies": 1,
    "queries": [
      { "param": "DS_STICKER", "query": "SELECT 1 AS test" }
    ]
  }'
```

---

## Gestión diaria de los servicios

Se utiliza el script `manage-services.ps1`:

```powershell
# Ver estado de ambos servicios
.\scripts\manage-services.ps1 -Action status

# Reiniciar ambos
.\scripts\manage-services.ps1 -Action restart

# Reiniciar solo uno
.\scripts\manage-services.ps1 -Action restart -Target lab
.\scripts\manage-services.ps1 -Action restart -Target gases

# Ver logs
.\scripts\manage-services.ps1 -Action logs -Target lab
.\scripts\manage-services.ps1 -Action logs -Target gases

# Detener ambos
.\scripts\manage-services.ps1 -Action stop

# Iniciar ambos
.\scripts\manage-services.ps1 -Action start
```

También pueden administrarse desde `services.msc` buscando `JaspertReport-Lab` y `JaspertReport-Gases`.

---

## Actualizar los servicios (nuevo build)

Cuando haya cambios en el código:

```powershell
# 1. Detener servicios
.\scripts\manage-services.ps1 -Action stop

# 2. Construir nuevo bundle
.\scripts\build-portable-exe.ps1

# 3. Reinstalar servicios
.\scripts\install-dual-services.ps1
```

O en un solo paso:
```powershell
.\scripts\build-and-deploy.ps1
```

> El script `install-dual-services.ps1` elimina automáticamente los servicios anteriores antes de reinstalar.

---

## Distribuir un servicio a otra máquina

Para llevar una instancia a la máquina destino no necesitas copiar el código fuente ni tener Maven ni JDK allá. El script `package-service.ps1` genera un ZIP autocontenido con todo lo necesario.

### En la máquina de desarrollo — Generar el paquete

```powershell
# Para laboratorio
.\scripts\package-service.ps1 -Instance lab

# Para gases
.\scripts\package-service.ps1 -Instance gases
```

El ZIP se genera en la carpeta `dist\` del proyecto:
```
dist\
├── JaspertReport-lab-package.zip
└── JaspertReport-gases-package.zip
```

Contenido del ZIP:
```
JaspertReport-lab-package.zip
├── bundle\              ← JaspertReport.exe + Java Runtime (no requiere Java en destino)
├── application.properties  ← config de esa instancia (puerto, BD, token, rutas)
├── reportes\            ← plantillas .jrxml y .jasper
└── install.ps1          ← script de instalación para ejecutar en destino
```

### En la máquina destino — Instalar el servicio

**Prerrequisito único:** NSSM instalado (`choco install nssm -y` o manual desde https://nssm.cc).

1. Copia el ZIP a la máquina destino
2. Extráelo en cualquier carpeta temporal (ej: `C:\temp\`)
3. Abre PowerShell **como Administrador**
4. Ejecuta:

```powershell
cd C:\temp\JaspertReport-lab-package
.\install.ps1
```

El script hace todo automáticamente:
- Crea `C:\servicios\lab\` (o `gases\`)
- Copia el bundle, `application.properties` y reportes
- Registra el servicio con NSSM con inicio automático
- Inicia el servicio y muestra el estado final

5. Verifica la salud:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"
```

> **Nota:** antes de empaquetar, **edita** el archivo `deploy\lab\application.properties` (o `deploy\gases\application.properties`) con la URL/credenciales de base de datos que corresponde a la máquina destino, el token que usará esa empresa y cualquier otro ajuste local. El ZIP contiene esa copia exacta, por lo que si olvidas cambiarlo el servicio levantará con la configuración de tu entorno de desarrollo. Mantén un control de versiones de las configuraciones específicas por cliente para evitar confusiones.

---

## Actualizar solo los reportes (.jrxml)

Si cambiaron plantillas pero no el código Java, no es necesario recompilar. Solo copia los archivos:

```powershell
# Laboratorio
Copy-Item src\main\resources\reportes\* C:\servicios\lab\reportes\ -Force

# Gases
Copy-Item src\main\resources\reportes\* C:\servicios\gases\reportes\ -Force
```

La aplicación recompila los `.jrxml` automáticamente al siguiente request si detecta que el `.jasper` es más antiguo.

---

## Solución de problemas

### Servicio queda en `SERVICE_PAUSED`

```powershell
nssm restart JaspertReport-Lab
nssm restart JaspertReport-Gases
```

Si persiste, revisar el stderr:
```powershell
Get-Content C:\servicios\lab\logs\stderr.log -Tail 30
Get-Content C:\servicios\gases\logs\stderr.log -Tail 30
```

### Log vacío y "Failed to launch JVM"

Verificar que el archivo `app\JaspertReport.cfg` **no contenga** la línea `spring.config.location`. Si la tiene, eliminarla:

```powershell
(Get-Content "C:\servicios\gases\app\JaspertReport.cfg") |
  Where-Object { $_ -notmatch "spring\.config\.location" } |
  Set-Content "C:\servicios\gases\app\JaspertReport.cfg"
```

Luego reiniciar: `nssm restart JaspertReport-Gases`

### El servicio levanta pero responde en el puerto equivocado

Verificar que el `application.properties` en la raíz del directorio del servicio tenga el `server.port` correcto:

```powershell
Get-Content C:\servicios\lab\application.properties | Select-String "server.port"
Get-Content C:\servicios\gases\application.properties | Select-String "server.port"
```

### Error de conexión a la base de datos

Revisar los logs de cada instancia y verificar que las credenciales en `application.properties` sean correctas. Asegurarse de que el servidor de BD esté accesible desde el servidor donde corre el servicio.

---

## Resumen de puertos y tokens

| Instancia | Servicio Windows | Puerto | Token | Endpoint principal |
|---|---|---|---|---|
| Laboratorio | `JaspertReport-Lab` | `8080` | `java-service-lab-2026` | `POST /reportes/generar` |
| Gases | `JaspertReport-Gases` | `8081` | `java-service-gases-2026` | `POST /reportes/imprimir` |
