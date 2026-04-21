# Despliegue Dual de Instancias — Arquitectura Multi-Servicio

Este documento describe el proceso completo para desplegar dos instancias independientes del motor de reportes JaspertReport como servicios Windows en la misma máquina, cada una conectada a una base de datos diferente.

---

## Arquitectura resultante

```
JaspertReport.jar (único binario)
    │
    ├─ Servicio: JaspertReport-Instance-A  → Puerto 8080
    │     C:\servicios\instancia-a\
    │     ├── JaspertReport.exe
    │     ├── application.properties        ← Configuración instancia A
    │     ├── reportes\                     ← Plantillas .jrxml de A
    │     └── logs\
    │
    └─ Servicio: JaspertReport-Instance-B  → Puerto 8081
          C:\servicios\instancia-b\
          ├── JaspertReport.exe
          ├── application.properties        ← Configuración instancia B
          ├── reportes\                     ← Plantillas .jrxml de B
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
│   ├── instance-a/
│   │   └── application.properties     ← Configuración instancia A
│   └── instance-b/
│       └── application.properties     ← Configuración instancia B
└── scripts/
    ├── build-portable-exe.ps1          ← Genera el bundle portable
    ├── install-dual-services.ps1       ← Instala ambas instancias
    ├── build-and-deploy.ps1            ← Build + install en un paso
    └── manage-services.ps1             ← Gestión diaria
```

---

## Configuración de cada instancia

### `deploy/instance-a/application.properties`

```properties
spring.application.name=JaspertReport-Instance-A
server.port=8080
server.address=0.0.0.0

service.token=your-instance-a-token-here
app.reportes.ruta=C:/servicios/instancia-a/reportes/

spring.datasource.url=jdbc:mysql://YOUR_DB_HOST:YOUR_DB_PORT/your_database_a?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=CONVERT_TO_NULL
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
```

### `deploy/instance-b/application.properties`

```properties
spring.application.name=JaspertReport-Instance-B
server.port=8081
server.address=0.0.0.0

service.token=your-instance-b-token-here
app.reportes.ruta=C:/servicios/instancia-b/reportes/

spring.datasource.url=jdbc:mysql://YOUR_DB_HOST:YOUR_DB_PORT/your_database_b?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=CONVERT_TO_NULL
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
```

> **Importante:** Ajusta los valores de `YOUR_DB_HOST`, `YOUR_DB_PORT`, `your_database_a`, `your_database_b`, `your_db_user`, `your_db_password`, y los tokens según tu entorno antes de desplegar.

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

Este script realiza por cada instancia:
1. Crea el directorio del servicio (`C:\servicios\instancia-a\` y `C:\servicios\instancia-b\`)
2. Copia el bundle portable completo
3. Copia el `application.properties` correspondiente al directorio raíz del servicio
4. Copia las plantillas `.jrxml` a la carpeta `reportes\`
5. Registra el servicio con NSSM con inicio automático y rotación de logs
6. Inicia ambos servicios

Resultado esperado:
```
[OK] Servicio 'JaspertReport-Instance-A' registrado correctamente.
[OK] Servicio 'JaspertReport-Instance-B' registrado correctamente.
```

---

### Paso 4 — Verificar que los servicios levantaron

Esperar ~15 segundos para que Spring Boot arranque completamente:

```powershell
Start-Sleep -Seconds 15
nssm status JaspertReport-Instance-A
nssm status JaspertReport-Instance-B
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
Get-Content C:\servicios\instancia-a\logs\stdout.log -Tail 20
Get-Content C:\servicios\instancia-b\logs\stdout.log -Tail 20
```

En el log de cada servicio deberías ver:
```
Tomcat started on port(s): 8080   ← en instancia A
Tomcat started on port(s): 8081   ← en instancia B
Started JaspertReportApplication in X.XXX seconds
```

---

### Paso 7 — Probar cada endpoint

**Instancia A** (`POST /reportes/generar`):
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/reportes/generar" `
  -Method POST `
  -Headers @{ "X-Service-Token" = "your-instance-a-token-here" } `
  -ContentType "application/json" `
  -Body '{
    "reportName": "YourReportName",
    "format": "PDF",
    "queries": [
      { "param": "DS_PARAM", "query": "SELECT 1 AS test" }
    ]
  }'
```

**Instancia B** (`POST /reportes/generar` o `/reportes/imprimir`):
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/reportes/generar" `
  -Method POST `
  -Headers @{ "X-Service-Token" = "your-instance-b-token-here" } `
  -ContentType "application/json" `
  -Body '{
    "reportName": "AnotherReport",
    "format": "PDF",
    "queries": [
      { "param": "DS_PARAM", "query": "SELECT 1 AS test" }
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
.\scripts\manage-services.ps1 -Action restart -Target instance-a
.\scripts\manage-services.ps1 -Action restart -Target instance-b

# Ver logs
.\scripts\manage-services.ps1 -Action logs -Target instance-a
.\scripts\manage-services.ps1 -Action logs -Target instance-b

# Detener ambos
.\scripts\manage-services.ps1 -Action stop

# Iniciar ambos
.\scripts\manage-services.ps1 -Action start
```

También pueden administrarse desde `services.msc` buscando `JaspertReport-Instance-A` y `JaspertReport-Instance-B`.

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

## Distribuir una instancia a otra máquina

Para llevar una instancia a la máquina destino no necesitas copiar el código fuente ni tener Maven ni JDK allá. El script `package-service.ps1` genera un ZIP autocontenido con todo lo necesario.

### En la máquina de desarrollo — Generar el paquete

```powershell
# Para instancia A
.\scripts\package-service.ps1 -Instance instance-a

# Para instancia B
.\scripts\package-service.ps1 -Instance instance-b
```

El ZIP se genera en la carpeta `dist\` del proyecto:
```
dist\
├── JaspertReport-instance-a-package.zip
└── JaspertReport-instance-b-package.zip
```

Contenido del ZIP:
```
JaspertReport-instance-a-package.zip
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
cd C:\temp\JaspertReport-instance-a-package
.\install.ps1
```

El script hace todo automáticamente:
- Crea `C:\servicios\instancia-a\` (o `instancia-b\`)
- Copia el bundle, `application.properties` y reportes
- Registra el servicio con NSSM con inicio automático
- Inicia el servicio y muestra el estado final

5. Verifica la salud:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"
```

> **Nota:** Antes de empaquetar, **edita** el archivo `deploy\instance-a\application.properties` (o `deploy\instance-b\application.properties`) con la URL/credenciales de base de datos que corresponde a la máquina destino, el token que usará esa instancia y cualquier otro ajuste local. El ZIP contiene esa copia exacta, por lo que si olvidas cambiarlo el servicio levantará con la configuración de tu entorno de desarrollo. Mantén un control de versiones de las configuraciones específicas por instancia para evitar confusiones.

---

## Actualizar solo los reportes (.jrxml)

Si cambiaron plantillas pero no el código Java, no es necesario recompilar. Solo copia los archivos:

```powershell
# Instancia A
Copy-Item src\main\resources\reportes\* C:\servicios\instancia-a\reportes\ -Force

# Instancia B
Copy-Item src\main\resources\reportes\* C:\servicios\instancia-b\reportes\ -Force
```

La aplicación recompila los `.jrxml` automáticamente al siguiente request si detecta que el `.jasper` es más antiguo.

---

## Solución de problemas

### Servicio queda en `SERVICE_PAUSED`

```powershell
nssm restart JaspertReport-Instance-A
nssm restart JaspertReport-Instance-B
```

Si persiste, revisar el stderr:
```powershell
Get-Content C:\servicios\instancia-a\logs\stderr.log -Tail 30
Get-Content C:\servicios\instancia-b\logs\stderr.log -Tail 30
```

### Log vacío y "Failed to launch JVM"

Verificar que el archivo `app\JaspertReport.cfg` **no contenga** la línea `spring.config.location`. Si la tiene, eliminarla:

```powershell
(Get-Content "C:\servicios\instancia-b\app\JaspertReport.cfg") |
  Where-Object { $_ -notmatch "spring\.config\.location" } |
  Set-Content "C:\servicios\instancia-b\app\JaspertReport.cfg"
```

Luego reiniciar: `nssm restart JaspertReport-Instance-B`

### El servicio levanta pero responde en el puerto equivocado

Verificar que el `application.properties` en la raíz del directorio del servicio tenga el `server.port` correcto:

```powershell
Get-Content C:\servicios\instancia-a\application.properties | Select-String "server.port"
Get-Content C:\servicios\instancia-b\application.properties | Select-String "server.port"
```

### Error de conexión a la base de datos

Revisar los logs de cada instancia y verificar que las credenciales en `application.properties` sean correctas. Asegurarse de que el servidor de BD esté accesible desde el servidor donde corre el servicio.

---

## Resumen de puertos y configuración

| Instancia | Servicio Windows | Puerto | Variable Importante | Endpoint principal |
|---|---|---|---|---|
| Instance A | `JaspertReport-Instance-A` | `8080` | `service.token` (token-a) | `POST /reportes/generar` |
| Instance B | `JaspertReport-Instance-B` | `8081` | `service.token` (token-b) | `POST /reportes/generar` |

Cada instancia debe tener su propio `application.properties` con valores únicos para:
- `server.port`
- `service.token`
- `spring.datasource.url` (base de datos diferente)
- `app.reportes.ruta` (carpeta de plantillas diferente)
