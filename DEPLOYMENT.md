# Deployment Guide — JasperReport

Guía para desplegar el motor de reportes multi-tenant en producción.

---

## 📋 Requisitos

- **Java 17+**
- **MySQL 8.0+** (una base de datos por tenant)
- **512 MB RAM** mínimo por instancia
- Puerto disponible (default: `8080`)

---

## 🚀 Quick Deploy — JAR

### 1. Construir el JAR

```bash
./mvnw clean package -DskipTests
```

El JAR se genera en `target/JasperReport-0.0.1-SNAPSHOT.jar`.

### 2. Crear estructura de despliegue

```bash
mkdir -p /opt/jasperreport/reportes/{default,acme,corp}
```

### 3. Configurar `application.properties`

```properties
# Modo de despliegue
app.profile=centralized

# Tenant default
app.tenants.default.service-token=${DEFAULT_TOKEN}
app.tenants.default.reportes-ruta=/opt/jasperreport/reportes/default/
app.tenants.default.datasource.url=jdbc:mysql://db-host:3306/db_default?useSSL=false&serverTimezone=UTC
app.tenants.default.datasource.username=${DEFAULT_DB_USER}
app.tenants.default.datasource.password=${DEFAULT_DB_PASS}
app.tenants.default.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
app.tenants.default.allowed-reports=MasterReport,DetailReport,SimpleReport
app.tenants.default.allowed-formats=PDF,XLSX,DOCX,HTML

# Tenant acme
app.tenants.acme.service-token=${ACME_TOKEN}
app.tenants.acme.reportes-ruta=/opt/jasperreport/reportes/acme/
app.tenants.acme.datasource.url=jdbc:mysql://db-host:3306/db_acme?useSSL=false&serverTimezone=UTC
app.tenants.acme.datasource.username=${ACME_DB_USER}
app.tenants.acme.datasource.password=${ACME_DB_PASS}
app.tenants.acme.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
app.tenants.acme.allowed-reports=ReporteVentas,StickerQR

server.port=8080
```

### 4. Ejecutar

```bash
java -jar JasperReport-0.0.1-SNAPSHOT.jar
```

---

## 🗂️ Modo Dedicado

Para tenants que requieran instancia propia (aislamiento físico, alta carga, requisitos contractuales):

```properties
app.profile=dedicated
app.assigned-tenant=acme

app.tenants.acme.service-token=${ACME_TOKEN}
app.tenants.acme.reportes-ruta=/opt/jasperreport/reportes/acme/
app.tenants.acme.datasource.url=jdbc:mysql://db-acme:3306/db_acme
# ...
```

La instancia dedicada **rechaza tokens de otros tenants** con HTTP 403.  
El mismo JAR funciona en ambos modos — solo cambia la configuración.

---

## 🔐 Seguridad

### Tokens por tenant

Cada tenant tiene su propio token. Usá variables de entorno, nunca hardcodees:

```bash
export DEFAULT_TOKEN="tok-seguro-default-2026"
export ACME_TOKEN="tok-seguro-acme-2026"
```

Configurá `application.properties` con placeholders:
```properties
app.tenants.default.service-token=${DEFAULT_TOKEN}
```

### SSL/HTTPS

```properties
server.ssl.key-store=/ruta/a/keystore.jks
server.ssl.key-store-password=${KEYSTORE_PASS}
server.ssl.key-store-type=JKS
server.port=8443
```

### Firewall

- Abrí solo el puerto de la aplicación para IPs autorizadas
- Las bases de datos de tenants deben ser accesibles solo desde la instancia del motor

---

## 📊 Pool de Conexiones

Cada tenant tiene su propio pool HikariCP. El sizing depende de la carga esperada por tenant.

Para ajustar el pool por tenant, usá propiedades adicionales en el `DataSourceManager` o externalizá la configuración vía `application.properties`:

```properties
# Ejemplo (requiere soporte en DataSourceManager)
app.tenants.acme.datasource.hikari.maximum-pool-size=10
app.tenants.acme.datasource.hikari.minimum-idle=2
```

---

## 🔧 Solución de Problemas

### "Failed to initialize pool: Communications link failure"

La base de datos del tenant no es alcanzable. Verificá:
- URL, usuario y contraseña en la config del tenant
- Que la BD esté corriendo y accesible desde la instancia

### "Unknown or invalid service token" (401)

El token no coincide con ningún tenant configurado.

### "Reporte no permitido para tenant" (403)

El reporte solicitado no está en el `allowed-reports` del tenant.

### "Template not found" (404)

El `.jrxml`/.`jasper` no existe en la carpeta `reportes-ruta` del tenant.  
Recordá: **no hay fallback a carpeta compartida**.

### Puerto en uso

```bash
java -jar JasperReport.jar --server.port=8081
```

---

## 📝 Logs

```bash
# DEBUG en runtime
java -jar JasperReport.jar --logging.level.com.example.JasperReport=DEBUG

# Logs a archivo (en application.properties)
logging.file.name=logs/jasperreport.log
logging.file.max-size=10MB
```

Los logs incluyen `tenantId` para trazabilidad. **Nunca** se loguean credenciales ni tokens completos.

---

## 🏗️ Estructura de Despliegue

```
/opt/jasperreport/
├── JasperReport.jar
├── application.properties
├── reportes/
│   ├── default/
│   │   ├── SimpleReport.jrxml
│   │   ├── MasterReport.jrxml
│   │   └── DetailReport.jrxml
│   ├── acme/
│   │   └── ReporteVentas.jrxml
│   └── corp/
│       └── ReporteCartera.jrxml
└── logs/
    └── jasperreport.log
```
