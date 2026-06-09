# Production Guide — JasperReport

Guía para operar el motor de reportes multi-tenant en producción.

---

## 🏗️ Arquitectura de Producción

```
                    ┌──────────────────┐
                    │  Cliente (PHP/JS) │
                    └────────┬─────────┘
                             │ HTTPS
                             ▼
                 ┌───────────────────────┐
                 │   Proxy Reverso        │
                 │   (Nginx / Caddy)      │
                 │   - SSL termination    │
                 │   - Rate limiting      │
                 └───────────┬───────────┘
                             │
              ┌──────────────┴──────────────┐
              │                             │
    ┌─────────▼────────┐          ┌────────▼─────────┐
    │  JasperReport    │          │  JasperReport    │
    │  (centralized)    │          │  (dedicated)      │
    │  :8080            │          │  :8081            │
    │  multi-tenant     │          │  dedicated        │
    └────┬────┬────┬────┘          └────────┬─────────┘
         │    │    │                        │
         ▼    ▼    ▼                        ▼
    ┌────────┐┌────────┐┌────────┐    ┌────────┐
    │DB def  ││DB acme ││DB corp │    │DB big  │
    └────────┘└────────┘└────────┘    └────────┘
```

---

## 🔐 Seguridad

### Tokens

- Cada tenant tiene su **propio token** de servicio
- Los tokens se pasan por **variables de entorno**, nunca hardcodeados
- Rotá los tokens periódicamente

```bash
export DEFAULT_TOKEN="$(openssl rand -hex 32)"
export ACME_TOKEN="$(openssl rand -hex 32)"
```

En `application.properties`:
```properties
app.tenants.default.service-token=${DEFAULT_TOKEN}
app.tenants.acme.service-token=${ACME_TOKEN}
```

### Autenticación

La autenticación es manejada por **interceptores** (`TokenValidator` + `TenantContextInitializer`), no por Spring Security.  
No es necesario configurar Spring Security. El flujo es:

1. `TokenValidator` verifica presencia y validez del header `X-Service-Token`
2. `TenantContextInitializer` resuelve el tenant, enforces dedicated mode y carga `TenantContext`
3. El token nunca se envía en la URL ni en el body — solo en el header

### Aislamiento entre tenants

- **Database-per-tenant**: cada tenant tiene su propia base de datos
- **HikariCP por tenant**: un pool de conexiones independiente por tenant
- **Templates aislados**: cada tenant tiene su propio directorio de reportes
- **Allowlist por tenant**: solo los reportes configurados son accesibles

### SSL/HTTPS

```properties
server.ssl.key-store=/etc/ssl/keystore.jks
server.ssl.key-store-password=${KEYSTORE_PASS}
server.ssl.key-store-type=JKS
server.port=8443
```

### Variables de entorno sensibles

| Variable | Uso |
|----------|-----|
| `DEFAULT_TOKEN` | Token del tenant default |
| `ACME_TOKEN` | Token del tenant acme |
| `ACME_DB_USER` | Usuario BD tenant acme |
| `ACME_DB_PASS` | Contraseña BD tenant acme |
| `KEYSTORE_PASS` | Contraseña del keystore SSL |

---

## ⚡ Rendimiento

### Pool de conexiones por tenant

Cada tenant tiene su propio `HikariCP`. El pool se crea en `DataSourceManager` al iniciar la aplicación.  
Para ajustar el tamaño del pool, configurá:

```java
// DataSourceManager.java — por defecto usa HikariCP defaults (max 10)
HikariConfig config = new HikariConfig();
config.setMaximumPoolSize(20);
config.setMinimumIdle(5);
```

**No** uses `spring.datasource.hikari.*` — esas propiedades son para el datasource global de Spring, no para los pools por tenant.

### Monitoreo

Spring Boot Actuator está incluido. Endpoints útiles:

```properties
management.endpoints.web.exposure.include=health,metrics,info
management.endpoint.health.show-details=always
```

```bash
curl http://localhost:8080/actuator/health
```

---

## 📊 Logs y Monitoreo

### Configuración de logs

```properties
# Nivel por paquete
logging.level.com.example.JasperReport=INFO
logging.level.com.example.JasperReport.tenant=DEBUG  # resolución de tenants

# Archivo rotativo
logging.file.name=logs/jasperreport.log
logging.logback.rollingpolicy.max-file-size=10MB
logging.logback.rollingpolicy.max-history=30
```

### Qué se loguea

- ✅ `tenantId` en cada operación
- ✅ Errores de compilación y generación
- ✅ Validación de datasources al iniciar
- ❌ Tokens completos (solo fingerprint)
- ❌ Queries SQL en errores (redactado)
- ❌ Contraseñas de bases de datos

---

## 🔄 Backup

### Reportes (.jrxml / .jasper)

Respaldá el directorio de reportes de cada tenant:
```bash
rsync -av /opt/jasperreport/reportes/ backup@storage:/backups/reportes/
```

### Bases de datos

Cada tenant tiene su propia BD. Respaldalas independientemente:
```bash
mysqldump -h db-host -u user -p db_default > backup_default_$(date +%Y%m%d).sql
mysqldump -h db-host -u user -p db_acme   > backup_acme_$(date +%Y%m%d).sql
```

---

## 🔧 Health Check

```bash
# Health endpoint
curl http://localhost:8080/actuator/health

# Verificar tenant específico
curl -X POST http://localhost:8080/reportes/generar \
  -H "X-Service-Token: ${ACME_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"reportName":"SimpleReport","format":"PDF","queries":[{"param":"DS_DATOS","query":"SELECT '"'"'1'"'"' AS id, '"'"'OK'"'"' AS nombre, '"'"'OK'"'"' AS valor"}]}'
```

---

## 🚨 Alertas

| Condición | Severidad | Acción |
|-----------|-----------|--------|
| `Failed to initialize pool` en startup | 🔴 CRITICAL | BD de tenant no alcanzable — corregir config |
| `Datasource OK` ausente para algún tenant | 🔴 CRITICAL | Validación falló en startup |
| 401 repetidos desde misma IP | 🟡 WARNING | Posible ataque de fuerza bruta |
| 403 repetidos para reporte legítimo | 🟡 INFO | Allowlist necesita actualización |
| Tiempo de respuesta > 5s | 🟡 WARNING | Revisar queries SQL del tenant o tamaño de pool |
