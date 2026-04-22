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
- **⚙️ Compilación Automática** - `.jrxml` se compila on-demand
- **🗂️ Configuración por Instancia** - Cada instancia conecta a su propia BD
- **🖨️ Impresión Directa** - Envía a impresoras del sistema sin exportar
- **🔒 Autenticación por Header** - Protección con `X-Service-Token`
- **🏗️ Arquitectura Extensible** - Patrón Strategy para exporters
- **📝 Logging Centralizado** - Trazabilidad completa
- **⚡ Rendimiento** - HikariCP, compilación en caché

---

## 🏗️ Arquitectura

### Flujo de Datos

```
┌─────────────┐
│   Cliente   │  POST /reportes/generar
│  (PHP, etc) │  {reportName, format, queries}
└──────┬──────┘
       │
       ▼
┌──────────────────────┐
│ ReportController     │  ← Validación + Token
└──────┬───────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│   ReportOrchestrator (Orquestador)   │
└──────┬───────────────────────────────┘
       │
   ┌───┴────────────────────────────────────┐
   │                                        │
   ▼                                        ▼
JasperFiller                        ExporterRegistry
   │                                        │
   ├─ Por cada query:                  ├─ getExporter(format)
   │  ├─ QueryExecutor.execute(sql)    │  │
   │  ├─ → List<Map<String,Object>>    │  └─ → ReportExporter
   │  └─ → JRMapCollectionDataSource   │     (Strategy Pattern)
   │                                    │
   └─ JasperFillManager.fillReport()   └─ export(jasperPrint)
      → JasperPrint                        → byte[]
```

### Estructura de Paquetes

```
src/main/java/com/example/JaspertReport/
├── controllers/
│   └── ReportController.java           ← HTTP endpoints
├── services/
│   ├── ReportOrchestrator.java         ← Coreografía
│   ├── JasperFiller.java               ← Rellena reportes
│   ├── QueryExecutor.java              ← Ejecuta SQL
│   ├── ReportPrintService.java         ← Impresión
│   └── exporters/
│       ├── ReportExporter.java         ← Interface
│       ├── PdfReportExporter.java
│       ├── ExcelReportExporter.java
│       ├── HtmlReportExporter.java
│       ├── DocxReportExporter.java
│       └── ExporterRegistry.java       ← Autodescubrimiento
├── dtos/
│   ├── ReportRequestDTO.java
│   ├── QueryParamDTO.java
│   ├── PrintRequestDTO.java
│   └── ReportResult.java
└── exceptions/
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

## 📖 Documentación por Audiencia

- **👤 Usuarios finales:** Ver [DEPLOYMENT.md](DEPLOYMENT.md)
  - Instalar ejecutable portable
  - Instalar JAR o Docker
  - Solución de problemas
  
- **👨‍💻 Desarrolladores:** Ver [DEVELOPMENT.md](DEVELOPMENT.md)
  - Setup de desarrollo local
  - Tests y debugging
  - Agregar nuevas features
  
- **🚀 DevOps/Producción:** Ver [PRODUCTION.md](PRODUCTION.md)
  - Deployment seguro
  - Multi-instancia
  - Monitoreo y backups

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
# Token de autenticación (cambiar en producción)
service.token=tu-token-secreto-aqui

# Ruta a reportes (debe existir y terminar en /)
app.reportes.ruta=/ruta/a/tus/reportes/

# Base de datos
spring.datasource.url=jdbc:mysql://localhost:3306/tu_base_datos?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=CONVERT_TO_NULL
spring.datasource.username=tu_usuario
spring.datasource.password=tu_contraseña

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
| `queries[].query` | String | ✅ | Query SQL a ejecutar |
| `queries[].datasource` | String | ❌ | Campo legacy: se ignora en la arquitectura actual |

**Respuestas:**

| Código | Descripción |
|--------|-------------|
| `200 OK` | Reporte generado correctamente (body = bytes del archivo) |
| `400 Bad Request` | Campos faltantes o queries vacías |
| `401 Unauthorized` | Token inválido o ausente |
| `404 Not Found` | El archivo `.jrxml` no existe |
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

### Paso 3: Guardar y copiar

1. Guarda como `MiReporte.jrxml`
2. Cópialo a `/ruta/a/tus/reportes/`
3. La aplicación lo compilará automáticamente al primer uso

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
# Ejecutar pruebas unitarias
mvn test

# Ejecutar todas las pruebas incluyendo integración
mvn verify
```

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

## 📚 Documentación Adicional

Ver la carpeta `docs/` para más detalles:

- **[Diseño del Motor de Reportes](docs/plans/2026-02-26-generic-report-engine-design.md)** - Decisiones arquitectónicas
- **[API REST](docs/API-Motor-Reportes.md)** - Endpoints y ejemplos
- **[Deployment Dual](docs/Despliegue-Dual-Servicios.md)** - Multi-instancia en Windows

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
