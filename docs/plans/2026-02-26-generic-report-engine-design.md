# Diseño: Motor Genérico de Reportes JasperReports

**Fecha:** 2026-02-26
**Estado:** Aprobado

---

## Contexto

La API actual expone un único endpoint `POST /reportes/atencion` acoplado a un reporte específico. Recibe 3 queries fijas, las mapea a DTOs tipados y siempre exporta en PDF. El objetivo es reemplazarlo por un motor genérico que soporte cualquier `.jasper`, N queries con nombre explícito y múltiples formatos de salida (PDF, XLSX).

---

## Decisiones de diseño

| Aspecto | Decisión |
|---|---|
| Formatos soportados | PDF y XLSX (extensible sin modificar código existente) |
| Mapeo de queries | Por nombre explícito (`param` + `query`) |
| Tipo de resultado de query | Siempre `List<Map<String,Object>>` → `JRMapCollectionDataSource` |
| Compatibilidad hacia atrás | Reemplazo completo — nuevo endpoint `POST /reportes/generar` |
| Patrón de extensibilidad | Strategy para exporters |

---

## Estructura de paquetes

```
com.example.JaspertReport/
├── controllers/
│   └── ReportController.java
├── services/
│   ├── ReportOrchestrator.java
│   ├── QueryExecutor.java
│   ├── JasperFiller.java
│   └── exporters/
│       ├── ReportExporter.java        (interface)
│       ├── PdfReportExporter.java
│       ├── ExcelReportExporter.java
│       └── ExporterRegistry.java
├── dtos/
│   ├── ReportRequestDTO.java
│   ├── QueryParamDTO.java
│   └── ReportResult.java
└── exceptions/
    ├── ReportNotFoundException.java
    ├── InvalidFormatException.java
    └── ReportGenerationException.java
```

---

## Contrato del endpoint

```
POST /reportes/generar
Header: X-Service-Token: <token>
Content-Type: application/json

{
  "reportName": "sample-report",
  "format": "PDF",
  "queries": [
    { "param": "DS_EMPRESA",   "query": "SELECT * FROM empresa WHERE row_id = 1" },
    { "param": "DS_ATENCION",  "query": "SELECT * FROM atencion WHERE codaten = '12345'" },
    { "param": "DS_NORMAL",    "query": "SELECT * FROM resultadosexamen WHERE tipores=1 AND codaten='12345'" },
    { "param": "DS_VARIABLES", "query": "SELECT * FROM resultadosexamen WHERE tipores=3 AND codaten='12345'" }
  ]
}
```

### Respuestas HTTP

| Código | Condición |
|---|---|
| 200 OK | Reporte generado correctamente |
| 400 Bad Request | Formato inválido, campos faltantes o queries vacías |
| 401 Unauthorized | Token incorrecto o ausente |
| 404 Not Found | El archivo `.jasper` no existe en disco |
| 500 Internal Server Error | Error al ejecutar SQL o generar el reporte |

---

## Flujo de datos

```
ReportController
    │  valida token y request (reportName, format, queries no vacíos)
    ▼
ReportOrchestrator.generate(request)
    │
    ├─▶ JasperFiller.fill(reportName, queries)
    │       │
    │       ├─ por cada QueryParamDTO:
    │       │     QueryExecutor.execute(sql) → List<Map<String,Object>>
    │       │     new JRMapCollectionDataSource(rows) → params[param]
    │       │
    │       ├─ params["SUBREPORT_DIR"] = reportesRuta
    │       └─ JasperFillManager.fillReport(.jasper, params, JREmptyDataSource) → JasperPrint
    │
    ├─▶ ExporterRegistry.getExporter(format) → ReportExporter
    │
    └─▶ ReportExporter.export(jasperPrint) → byte[]
            │
            └─▶ ReportResult(content, contentType, fileExtension)
```

---

## Interface Strategy y registro de exporters

```java
public interface ReportExporter {
    String getSupportedFormat();   // "PDF", "XLSX"
    String getContentType();       // "application/pdf", "application/vnd.openxmlformats..."
    String getFileExtension();     // "pdf", "xlsx"
    byte[] export(JasperPrint jasperPrint) throws JRException;
}
```

El `ExporterRegistry` recibe automáticamente todos los beans que implementen `ReportExporter` vía inyección de lista en Spring. No hay `if/switch` por formato en ninguna parte del código.

Agregar un nuevo formato = crear una clase que implemente `ReportExporter` y anotarla con `@Component`.

---

## Impacto en archivos JRXML existentes

Los parámetros tipados `DATOS_EMPRESA` (`DatosEmpresaDTO`) y `DATOS_ATENCION` (`AtencionDTO`) se reemplazan por `DS_EMPRESA` y `DS_ATENCION` de tipo `net.sf.jasperreports.engine.JRDataSource`.

Los subreportes `Normal.jrxml` y `Variables.jrxml` ya reciben un datasource — el cambio es mínimo (reemplazo de tipo de campo en la definición de parámetros del subreporte).

La lógica de filtrado por `tipores` que hoy vive en Java se elimina; el filtro pasa a las queries SQL enviadas por el cliente PHP.

---

## Clases eliminadas

- `LegacyReportController.java` — reemplazado por `ReportController.java`
- `LegacyReportService.java` — reemplazado por `ReportOrchestrator.java` + `JasperFiller.java`
- `ReportDataService.java` — reemplazado por `QueryExecutor.java`
- DTOs tipados: `DatosEmpresaDTO.java`, `AtencionDTO.java`, `ResultadoExamenDTO.java`

`ReportRequestDTO.java` se actualiza (ya no es una lista de strings — pasa a lista de `QueryParamDTO`).

---

## Principios SOLID aplicados

- **SRP:** Cada clase tiene una sola razón para cambiar.
- **OCP:** Agregar un nuevo formato no modifica ninguna clase existente.
- **LSP:** Cualquier `ReportExporter` puede sustituirse por otro sin alterar el comportamiento del orquestador.
- **ISP:** La interface `ReportExporter` es pequeña y enfocada.
- **DIP:** `ReportOrchestrator` depende de la abstracción `ReportExporter`, no de `PdfReportExporter` directamente.
