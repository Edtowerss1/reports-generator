# JaspertReport - Motor Genérico de Reportes Dinámicos

> Un motor de reportes escalable y extensible basado en **JasperReports** y **Spring Boot** que permite generar múltiples reportes sin cambios en el código.

[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=flat-square&logo=java)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.12-6DB33F?style=flat-square&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![JasperReports](https://img.shields.io/badge/JasperReports-7.0.3-E31937?style=flat-square)](https://community.jaspersoft.com/project/jasperreports-library)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

---

## 🎯 ¿Qué problema resuelve?

Tradicionalmente, cada nuevo reporte requería:
- ✗ Crear un nuevo endpoint HTTP
- ✗ Definir DTOs tipados
- ✗ Hardcodear queries SQL
- ✗ Modificar código y desplegar

**Con JaspertReport:**
- ✅ Un endpoint genérico = N reportes
- ✅ Queries dinámicas desde el cliente
- ✅ Parámetros nombrados explícitamente
- ✅ Agregar reporte = copiar `.jrxml` a carpeta

---

## 🌟 Características

- **🎨 Generación Dinámica** - Múltiples reportes sin tocar código
- **📊 Multi-Formato** - PDF, XLSX, DOCX, HTML (extensible)
- **⚙️ Compilación Automática** - `.jrxml` se compila on-demand por tenant
- **🏢 Multi-Tenant** - Múltiples clientes desde una sola instancia, con BD y reportes aislados por tenant
- **🗂️ Modos de Despliegue** - Centralizado (multi-tenant) o Dedicado (instancia por cliente)
- **🖨️ Impresión Directa** - Envía a impresoras del sistema sin exportar
- **🔒 Autenticación por Token** - Token por tenant validado por interceptores
- **🏗️ Arquitectura SOLID** - Interfaces con constructor injection, extensible sin modificar código
- **📝 Logging Centralizado** - Trazabilidad completa por tenant

---

## 🏗️ Arquitectura

### Flujo de Datos

```
┌─────────────┐
│   Cliente   │  POST /reportes/generar
│  (PHP, etc) │  X-Service-Token: <tenant-token>
└──────┬──────┘
       │
       ▼
┌──────────────────────┐
│ TokenValidator       │  ← Valida presencia del token
│ (Interceptor)        │     Delega en TenantResolver
└──────┬───────────────┘
       │  401 si inválido
       ▼
┌──────────────────────┐
│ TenantContextInit    │  ← Resuelve tenant, enforces dedicated mode
│ (Interceptor)        │     Popula TenantContext (ThreadLocal)
└──────┬───────────────┘
       │  403 si no autorizado
       ▼
┌──────────────────────┐
│ ReportController     │  ← Valida body, delega a orchestrator
└──────┬───────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│   ReportOrchestrator (Orquestador)   │  ← Allowlist enforcement por tenant
└──────┬───────────────────────────────┘
       │
   ┌───┴────────────────────────────────────┐
   │                                        │
   ▼                                        ▼
JasperFiller                        ExporterRegistry
   │                                        │
   ├─ TemplateResolver (tenant path)   ├─ getExporter(format)
   ├─ ReportCompiler (lazy)            │
   ├─ Por cada query:                  └─ → ReportExporter
   │  ├─ DataSourceProvider.getTemplate(tenantId)
   │  ├─ → List<Map<String,Object>>
   │  └─ → JRMapCollectionDataSource
   │
   └─ JasperFillManager.fillReport()
      → JasperPrint                    → export(jasperPrint)
                                           → byte[]
```

### Estructura de Paquetes

```
src/main/java/com/example/JaspertReport/
├── config/
│   ├── TenantProperties.java             ← @ConfigurationProperties multi-tenant
│   └── WebConfig.java                    ← Registro de interceptores
├── controllers/
│   └── ReportController.java            ← HTTP endpoints
├── tenant/
│   ├── Tenant.java                       ← Value record (id, ruta, datasource, allowlist)
│   ├── TenantContext.java                ← ThreadLocal holder
│   ├── TenantResolver.java               ← Interface: token → tenant
│   ├── ConfigBasedTenantResolver.java    ← Implementación por configuración
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
│   ├── ConfigBasedAllowlistService.java ← Implementación por configuración
│   ├── TemplateResolver.java            ← Interface: path de templates
│   ├── TenantScopedTemplateResolver.java← Path por tenant
│   ├── ReportCompiler.java              ← Interface: compilación lazy
│   ├── LazyReportCompiler.java          ← Timestamp-based compilation
│   └── exporters/
│       ├── ReportExporter.java          ← Interface
│       ├── PdfReportExporter.java
│       ├── ExcelReportExporter.java
│       ├── HtmlReportExporter.java
│       ├── DocxReportExporter.java
│       └── ExporterRegistry.java        ← Autodescubrimiento
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

## 📋 Requisitos Previos

- **Java 17** o superior
- **Maven 3.8+** (o usar `mvnw`)
- **MySQL 8.0+**
- **JasperStudio** (opcional, para diseñar reportes)

---

## 📖 Documentación

- **[README](README.md)** — esta guía: instalación, uso, arquitectura
- **[DEPLOYMENT.md](DEPLOYMENT.md)** — despliegue multi-tenant en producción
- **[DEVELOPMENT.md](DEVELOPMENT.md)** — setup de desarrollo, tests, extensibilidad
- **[PRODUCTION.md](PRODUCTION.md)** — seguridad, monitoreo, backups
- **[CONTRIBUTING.md](CONTRIBUTING.md)** — guía de contribución
- **[Postman Collection](postman/)** — colección de requests para probar la API

---

## 🚀 Instalación y Configuración (Quick Start)

### 1. Clonar el repositorio

```bash
git clone https://github.com/Edtowerss1/reports-generator.git
cd reports-generator
```

### 2. Configurar variables de entorno

Copia el archivo de ejemplo y ajusta según tu entorno:

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Edita `src/main/resources/application.properties`:

```properties
# Modo de despliegue: centralized (multi-tenant) o dedicated (instancia única)
app.profile=centralized

# Tenant por defecto
app.tenants.default.service-token=tu-token-secreto-aqui
app.tenants.default.reportes-ruta=/ruta/a/tus/reportes/
app.tenants.default.datasource.url=jdbc:mysql://localhost:3306/tu_base_datos?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=CONVERT_TO_NULL
app.tenants.default.datasource.username=tu_usuario
app.tenants.default.datasource.password=tu_contraseña
app.tenants.default.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
app.tenants.default.allowed-reports=MasterReport,DetailReport,SimpleReport
app.tenants.default.allowed-formats=PDF,XLSX,DOCX,HTML

# Para agregar más tenants, repetir el bloque con otro ID:
# app.tenants.cliente2.service-token=token-cliente-2
# app.tenants.cliente2.reportes-ruta=/reportes/cliente2/
# app.tenants.cliente2.datasource.url=jdbc:mysql://host2:3306/bd_cliente2
# ...

# Servidor
server.port=8080
```

### 3. Crear la estructura de reportes

```bash
mkdir -p /ruta/a/tus/reportes/
```

### 4. Compilar el proyecto

```bash
mvn clean package
```

O si usas Windows (sin Maven instalado):

```powershell
.\mvnw.cmd clean package
```

---

## 📖 Uso

### Generar un Reporte

```bash
curl -X POST http://localhost:8080/reportes/generar \
  -H "X-Service-Token: tu-token-secreto-aqui" \
  -H "Content-Type: application/json" \
  -d '{
    "reportName": "MiReporte",
    "format": "PDF",
    "queries": [
      {
        "param": "DS_EMPRESA",
        "query": "SELECT id, nombre, nit FROM empresa WHERE id = 1",
        "datasource": "default"
      },
      {
        "param": "DS_DATOS",
        "query": "SELECT * FROM datos WHERE estado = \u0027activo\u0027"
      }
    ]
  }'
```

**Parámetros de la solicitud:**

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `reportName` | String | ✅ | Nombre del archivo `.jrxml` (sin extensión) |
| `format` | String | ✅ | Formato de salida: `PDF`, `XLSX`, `DOCX`, `HTML` |
| `queries` | Array | ✅ | Lista de queries con parámetros |
| `queries[].param` | String | ✅ | Nombre del parámetro en el reporte (ej: `DS_EMPRESA`) |
| `queries[].query` | String | ✅ | Query SQL a ejecutar contra la BD del tenant |

**Respuestas:**

| Código | Descripción |
|--------|-------------|
| `200 OK` | Reporte generado correctamente (body = bytes del archivo) |
| `400 Bad Request` | Campos faltantes o queries vacías |
| `401 Unauthorized` | Token inválido, desconocido o ausente |
| `403 Forbidden` | Reporte no permitido para este tenant |
| `404 Not Found` | El archivo `.jrxml` no existe en el directorio del tenant |
| `500 Server Error` | Error en SQL o generación |

### Imprimir un Reporte

```bash
curl -X POST http://localhost:8080/reportes/imprimir \
  -H "X-Service-Token: tu-token-secreto-aqui" \
  -H "Content-Type: application/json" \
  -d '{
    "reportName": "MiReporte",
    "printerName": "Mi Impresora",
    "copies": 1,
    "queries": [
      {
        "param": "DS_DATOS",
        "query": "SELECT * FROM datos WHERE id = 123"
      }
    ]
  }'
```

---

## 🎨 Crear un Nuevo Reporte

### Paso 1: Diseñar en JasperStudio

1. Abre **JasperStudio** (descárgalo de [jaspersoft.com](https://www.jaspersoft.com/))
2. Crea un nuevo reporte `.jrxml`
3. Define **parámetros** de tipo `JRDataSource`:
   - Nombre: `DS_EMPRESA`, `DS_DATOS`, etc.
   - Clase: `net.sf.jasperreports.engine.JRDataSource`

### Paso 2: Usar parámetros en el reporte

En JasperStudio, puedes referenciar campos así:

```java
// En un campo de texto, iterar sobre el datasource
((net.sf.jasperreports.engine.data.JRMapCollectionDataSource)$P{DS_DATOS})
  .getData().stream()
  .map(m -> m.get("nombre"))
  .collect(Collectors.joining(", "))
```

### Paso 3: Guardar y copiar al directorio del tenant

1. Guarda como `MiReporte.jrxml`
2. Cópialo a la carpeta configurada para tu tenant (`app.tenants.<id>.reportes-ruta`)
3. La aplicación lo compilará automáticamente al primer uso (compilación lazy)
4. Los subreportes deben estar en el mismo directorio del tenant (`SUBREPORT_DIR` se resuelve por tenant)

### Paso 4: Llamar desde cliente

```javascript
// Ejemplo: JavaScript/Fetch API
fetch('/reportes/generar', {
  method: 'POST',
  headers: {
    'X-Service-Token': 'tu-token',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    reportName: 'MiReporte',
    format: 'PDF',
    queries: [
      {
        param: 'DS_DATOS',
        query: 'SELECT * FROM tabla WHERE condicion'
      }
    ]
  })
})
.then(res => res.blob())
.then(blob => {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'reporte.pdf';
  a.click();
});
```

---

## 🧪 Testing

```bash
# Ejecutar todas las pruebas (103 tests, JUnit 5 + H2)
./mvnw test
```

### Desarrollo local sin MySQL

El proyecto incluye un perfil `test` con H2 en memoria:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
```

Esto levanta 3 tenants (`default`, `acme`, `corp`) con bases H2 independientes.  
Usá la [colección de Postman](postman/) para probar los endpoints sin configurar MySQL.

---

## 🗂️ Modos de Despliegue

| Modo | `app.profile` | Descripción |
|------|--------------|-------------|
| **Centralizado** | `centralized` | Una instancia atiende múltiples tenants. Cada request resuelve el tenant desde el token. |
| **Dedicado** | `dedicated` | Una instancia por tenant. `app.assigned-tenant` define cuál. Rechaza tokens de otros tenants (403). |

Ambos modos usan **el mismo binario**. Solo cambia la configuración.

---

## 🤝 Extensibilidad

### Agregar un nuevo formato de exportación

Crea una clase que implemente `ReportExporter`:

```java
package com.example.JaspertReport.services.exporters;

import net.sf.jasperreports.engine.JasperPrint;
import org.springframework.stereotype.Component;

@Component
public class CsvReportExporter implements ReportExporter {
    
    @Override
    public String getSupportedFormat() {
        return "CSV";
    }
    
    @Override
    public String getContentType() {
        return "text/csv";
    }
    
    @Override
    public String getFileExtension() {
        return "csv";
    }
    
    @Override
    public byte[] export(JasperPrint jasperPrint) throws JRException {
        // Implementar lógica de exportación
        return new byte[0];
    }
}
```

Listo. El `ExporterRegistry` lo detectará automáticamente.

---

## 🐛 Reportar Bugs

Si encuentras un bug:

1. Abre una [Issue](https://github.com/Edtowerss1/reports-generator/issues)
2. Incluye:
   - Versión de Java y Spring Boot
   - Pasos para reproducir
   - Logs relevantes

---

## 📝 Licencia

Este proyecto está bajo la licencia **MIT**. Ver [LICENSE](LICENSE) para más detalles.

---

## 🙌 Contribuciones

¡Las contribuciones son bienvenidas! Por favor:

1. Haz fork del proyecto
2. Crea una rama (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

Ver [CONTRIBUTING.md](CONTRIBUTING.md) para más detalles.

---

## 📞 Contacto & Soporte

- **Issues & Bugs**: [GitHub Issues](https://github.com/Edtowerss1/reports-generator/issues)
- **Discussions**: [GitHub Discussions](https://github.com/Edtowerss1/reports-generator/discussions)

---

**Hecho con ❤️ para la comunidad de Java y reportes dinámicos**
