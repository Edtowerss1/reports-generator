# Development Guide — JasperReport

Guía para desarrolladores: setup local, arquitectura, tests y extensibilidad.

---

## 🔧 Requisitos

- **Java 17+** (JDK)
- **Maven 3.8+** (o `./mvnw` incluido)
- **Git**
- IDE recomendado: IntelliJ IDEA, Eclipse, o VS Code con Extension Pack for Java

---

## 📥 Setup Local

### 1. Clonar

```bash
git clone https://github.com/Edtowerss1/reports-generator.git
cd reports-generator
```

### 2. Desarrollo sin MySQL (perfil H2)

El proyecto incluye un perfil `test` con H2 en memoria y 3 tenants preconfigurados:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
```

Esto levanta la app en `http://localhost:8080` con:
- Tenant `default`: token `dev-token-2026`
- Tenant `acme`: token `tok-A`
- Tenant `corp`: token `tok-B`

Usá la [colección de Postman](postman/) para probar.

### 3. Con MySQL real

Creá `src/main/resources/application.properties` con la config multi-tenant:

```properties
app.profile=centralized

app.tenants.default.service-token=dev-token-2026
app.tenants.default.reportes-ruta=src/main/resources/reportes/
app.tenants.default.datasource.url=jdbc:mysql://localhost:3306/jasperreport_dev?useSSL=false&serverTimezone=UTC
app.tenants.default.datasource.username=root
app.tenants.default.datasource.password=root
app.tenants.default.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
app.tenants.default.allowed-reports=SimpleReport,MasterReport,DetailReport
app.tenants.default.allowed-formats=PDF,XLSX,DOCX,HTML

server.port=8080
```

```bash
./mvnw spring-boot:run
```

---

## 🏗️ Estructura del Proyecto

```
src/main/java/com/example/JasperReport/
├── config/
│   ├── TenantProperties.java             ← @ConfigurationProperties multi-tenant
│   └── WebConfig.java                    ← Registro de interceptores
├── controllers/
│   └── ReportController.java            ← HTTP endpoints (thin, auth via interceptors)
├── tenant/
│   ├── Tenant.java                       ← Value record
│   ├── TenantContext.java                ← ThreadLocal holder
│   ├── TenantResolver.java               ← Interface: token → tenant
│   ├── ConfigBasedTenantResolver.java    ← Token lookup from config
│   ├── TokenValidator.java               ← Interceptor SRP: valida header
│   ├── TenantContextInitializer.java     ← Interceptor SRP: resuelve tenant
│   ├── DataSourceProvider.java           ← Interface: tenant → JdbcTemplate
│   └── DataSourceManager.java            ← HikariCP por tenant
├── services/
│   ├── ReportOrchestrator.java          ← Coreografía + allowlist
│   ├── JasperFiller.java                ← Rellena reportes (tenant-aware)
│   ├── QueryExecutor.java               ← Ejecuta SQL (tenant-aware)
│   ├── ReportPrintService.java          ← Impresión
│   ├── ReportAllowlistService.java      ← Interface: validación de reportes
│   ├── ConfigBasedAllowlistService.java ← Implementación
│   ├── TemplateResolver.java            ← Interface: path de templates
│   ├── TenantScopedTemplateResolver.java← Path por tenant
│   ├── ReportCompiler.java              ← Interface: compilación lazy
│   ├── LazyReportCompiler.java          ← Timestamp-based
│   └── exporters/                       ← Strategy pattern
│       ├── ReportExporter.java
│       ├── PdfReportExporter.java
│       ├── ExcelReportExporter.java
│       ├── HtmlReportExporter.java
│       ├── DocxReportExporter.java
│       └── ExporterRegistry.java
├── dtos/
│   ├── ReportRequestDTO.java
│   ├── QueryParamDTO.java
│   ├── PrintRequestDTO.java
│   └── ReportResult.java
└── exceptions/
    ├── TenantResolutionException.java    ← 401
    ├── ReportNotAllowedException.java    ← 403
    ├── ReportNotFoundException.java
    ├── InvalidFormatException.java
    └── ReportGenerationException.java
```

---

## 🧪 Tests

```bash
# Todos los tests (103 tests, JUnit 5 + H2)
./mvnw test

# Test específico
./mvnw test -Dtest=ReportControllerTest

# Test de integración
./mvnw test -Dtest=MultiTenantE2ETest
```

---

## 🎨 Extensibilidad — SOLID

### Agregar un nuevo formato de exportación

Implementá `ReportExporter` y anotalo con `@Component`. El `ExporterRegistry` lo detecta automáticamente:

```java
@Component
public class CsvReportExporter implements ReportExporter {
    @Override public String getSupportedFormat() { return "CSV"; }
    @Override public String getContentType() { return "text/csv"; }
    @Override public String getFileExtension() { return "csv"; }
    @Override public byte[] export(JasperPrint jasperPrint) throws JRException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JRCsvExporter exporter = new JRCsvExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleWriterExporterOutput(baos));
        exporter.exportReport();
        return baos.toByteArray();
    }
}
```

### Agregar una nueva fuente de tenants

Implementá `TenantResolver`. La interfaz tiene dos métodos:

```java
public interface TenantResolver {
    Tenant resolve(String token);
    boolean validate(String token);
}
```

Ejemplo: `DatabaseTenantResolver` que lea tenants de una tabla en vez del archivo de configuración.

### Agregar una nueva estrategia de compilación

Implementá `ReportCompiler`. Ejemplo: `CacheOnlyReportCompiler` que solo use `.jasper` precompilados, sin compilar `.jrxml` en runtime.

---

## 📝 Convenciones

### Commits

Usamos [Conventional Commits](https://www.conventionalcommits.org/):

```bash
feat: add CSV exporter
fix: null pointer in QueryExecutor
docs: update API reference
refactor: simplify ReportOrchestrator
test: add tests for TenantResolver
```

### SOLID

- **Constructor injection** — nunca `@Autowired` en campos
- **Interfaces para puntos de extensión** — `TenantResolver`, `ReportExporter`, etc.
- **SRP** — una clase, una responsabilidad (`TokenValidator` ≠ `TenantContextInitializer`)

---

## 🐛 Debugging

```bash
# DEBUG en runtime
./mvnw spring-boot:run -Dspring-boot.run.profiles=test -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

```properties
# Log detallado en application.properties
logging.level.com.example.JasperReport=DEBUG
```

---

## 🆘 Problemas Comunes

| Error | Causa | Solución |
|-------|-------|----------|
| `Failed to initialize pool` | Datasource del tenant no alcanzable | Verificá URL y credenciales en `app.tenants.<id>.datasource.*` |
| `Unknown service token` (401) | Token no registrado | Verificá `app.tenants.<id>.service-token` |
| `Reporte no permitido` (403) | Reporte fuera del allowlist | Agregalo a `app.tenants.<id>.allowed-reports` |
| `Template not found` (404) | `.jrxml` no existe en el dir del tenant | Verificá `app.tenants.<id>.reportes-ruta` |
| `Port 8080 already in use` | Puerto ocupado | `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` |
