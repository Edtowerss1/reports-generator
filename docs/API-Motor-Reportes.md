# Motor Generador de Reportes — API REST

## Tabla de contenido

1. [Qué es y para qué sirve](#1-qué-es-y-para-qué-sirve)
2. [Stack tecnológico](#2-stack-tecnológico)
3. [Estructura del proyecto](#3-estructura-del-proyecto)
4. [Cómo funciona paso a paso](#4-cómo-funciona-paso-a-paso)
5. [El endpoint](#5-el-endpoint)
6. [Arquitectura y capas](#6-arquitectura-y-capas)
7. [Plantillas JRXML y subreportes](#7-plantillas-jrxml-y-subreportes)
8. [Cómo agregar un nuevo formato de exportación](#8-cómo-agregar-un-nuevo-formato-de-exportación)
9. [Cómo agregar un nuevo reporte](#9-cómo-agregar-un-nuevo-reporte)
10. [Configuración](#10-configuración)
11. [Ejemplo completo con Postman](#11-ejemplo-completo-con-postman)
12. [Códigos de respuesta HTTP](#12-códigos-de-respuesta-http)
13. [Preguntas frecuentes](#13-preguntas-frecuentes)

---

## 1. Qué es y para qué sirve

Esta API es un **motor genérico de generación de reportes**. Recibe por HTTP:

- El **nombre** del reporte (plantilla `.jasper`)
- El **formato** de salida (PDF, XLSX, HTML, DOCX)
- Las **consultas SQL** que alimentan el reporte

Y devuelve el archivo generado listo para descargar o mostrar en el navegador.

La API no sabe nada sobre el contenido de los reportes. Solo ejecuta las queries, inyecta los datos en la plantilla y exporta. Esto la hace **reutilizable para cualquier tipo de reporte** sin modificar código Java.

---

## 2. Stack tecnológico

| Tecnología       | Versión  | Para qué se usa                          |
|------------------|----------|------------------------------------------|
| Java             | 17       | Lenguaje base                            |
| Spring Boot      | 3.4.12   | Framework web + inyección de dependencias|
| JasperReports    | 7.0.3    | Motor de reportes (compilar, llenar, exportar) |
| MySQL Connector  | runtime  | Driver de conexión a base de datos       |
| Lombok           | 1.18.28  | Reducir código repetitivo (getters, setters) |
| Maven            | 3+       | Gestión de dependencias y build          |

---

## 3. Estructura del proyecto

```
src/main/java/com/example/JaspertReport/
│
├── JaspertReportApplication.java        ← Punto de entrada
│
├── controllers/
│   └── ReportController.java            ← Recibe la petición HTTP
│
├── dtos/
│   ├── ReportRequestDTO.java            ← Cuerpo del JSON que llega
│   ├── QueryParamDTO.java               ← Cada query con su nombre
│   └── ReportResult.java                ← Resultado: bytes + tipo + extensión
│
├── services/
│   ├── ReportOrchestrator.java          ← Orquesta todo el flujo
│   ├── JasperFiller.java                ← Compila plantillas + llena reportes
│   ├── QueryExecutor.java               ← Ejecuta SQL contra la BD
│   └── exporters/
│       ├── ReportExporter.java          ← Interfaz (contrato)
│       ├── PdfReportExporter.java       ← Exporta a PDF
│       ├── ExcelReportExporter.java     ← Exporta a XLSX
│       ├── HtmlReportExporter.java      ← Exporta a HTML
│       ├── DocxReportExporter.java      ← Exporta a DOCX (Word)
│       └── ExporterRegistry.java        ← Registro automático de exportadores
│
└── exceptions/
    ├── GlobalExceptionHandler.java      ← Manejo centralizado de errores
    ├── ReportNotFoundException.java     ← Reporte no existe → 404
    ├── InvalidFormatException.java      ← Formato no soportado → 400
    └── ReportGenerationException.java   ← Error al generar → 500


src/main/resources/
├── application.properties               ← Configuración (BD, token, rutas)
├── jasperreports_extension.properties   ← Registro de fuentes personalizadas
├── fonts/
│   ├── fonts.xml                        ← Definición de familia Arial
│   ├── arial.ttf, arialbd.ttf, ...      ← Archivos de fuente
└── reportes/
    ├── Laboratorio.jrxml / .jasper      ← Reporte principal
    ├── Paciente.jrxml / .jasper         ← Subreporte: datos del paciente
    ├── Variables.jrxml / .jasper        ← Subreporte: resultados tipo variable
    ├── Normal.jrxml / .jasper           ← Subreporte: resultados normales
    └── Logo.jpg                         ← Logo de la empresa
```

---

## 4. Cómo funciona paso a paso

Este es el flujo completo desde que llega una petición hasta que se devuelve el archivo:

```
Cliente (PHP, Postman, etc.)
    │
    │  POST /reportes/generar
    │  Headers: X-Service-Token, Content-Type: application/json
    │  Body: { reportName, format, queries[] }
    │
    ▼
┌─────────────────────────────────┐
│  1. ReportController            │  Valida el token y el cuerpo
└──────────────┬──────────────────┘
               ▼
┌─────────────────────────────────┐
│  2. ReportOrchestrator          │  Coordina llenar + exportar
└──────────────┬──────────────────┘
               ▼
┌─────────────────────────────────┐
│  3. JasperFiller                │
│     Para cada query:            │
│       → QueryExecutor.execute() │  Ejecuta el SQL en MySQL
│       → Empaqueta resultado en  │
│         JRMapCollectionDataSource│
│     Llena la plantilla .jasper  │
│     con los datos               │
└──────────────┬──────────────────┘
               ▼
┌─────────────────────────────────┐
│  4. ExporterRegistry            │  Busca el exportador por formato
│     → PdfReportExporter         │  (si format = "PDF")
│     → ExcelReportExporter       │  (si format = "XLSX")
└──────────────┬──────────────────┘
               ▼
┌─────────────────────────────────┐
│  5. ReportExporter.export()     │  Convierte a bytes (PDF o XLSX)
└──────────────┬──────────────────┘
               ▼
        Respuesta HTTP 200
        Content-Type: application/pdf
        Body: [bytes del archivo]
```

---

## 5. El endpoint

### `POST /reportes/generar`

#### Headers requeridos

| Header            | Valor                              | Descripción                |
|-------------------|------------------------------------|----------------------------|
| `X-Service-Token` | El token configurado en el servidor| Autenticación del servicio |
| `Content-Type`    | `application/json`                 | Tipo del cuerpo            |

#### Cuerpo (JSON)

```json
{
  "reportName": "Laboratorio",
  "format": "PDF",
  "queries": [
    {
      "param": "DS_EMPRESA",
      "query": "SELECT * FROM Datos_Empresa"
    },
    {
      "param": "DS_ATENCION",
      "query": "SELECT t1.*, ... FROM Mae_Atencion t1 WHERE ..."
    }
  ]
}
```

| Campo        | Tipo             | Obligatorio | Descripción                                                |
|--------------|------------------|-------------|------------------------------------------------------------|
| `reportName` | String           | Sí          | Nombre de la plantilla sin extensión (ej: `"Laboratorio"`) |
| `format`     | String           | Sí          | Formato de salida: `"PDF"`, `"XLSX"`, `"HTML"`, `"DOCX"`    |
| `queries`    | Array de objetos | Sí          | Al menos una query                                         |
| `queries[].param` | String      | Sí          | Nombre del parámetro en el reporte `.jrxml`                |
| `queries[].query` | String      | Sí          | Consulta SQL a ejecutar contra la BD                       |

**Importante:** El valor de `param` debe coincidir exactamente con el nombre del `<parameter>` definido en el archivo `.jrxml` del reporte.

#### Respuesta exitosa (200)

- `Content-Type`: `application/pdf` o `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- `Content-Disposition`: `inline; filename=reporte.pdf` o `reporte.xlsx`
- **Body**: los bytes del archivo generado

---

## 6. Arquitectura y capas

La API sigue los principios **SOLID** y está organizada en capas con responsabilidades claras:

### Capa 1 — Controller (`ReportController`)

Solo se encarga de:
- Recibir la petición HTTP
- Validar el token (`X-Service-Token`)
- Validar que el cuerpo tenga los campos requeridos
- Delegar al orquestador
- Devolver la respuesta HTTP

**No contiene lógica de negocio.**

### Capa 2 — Orquestador (`ReportOrchestrator`)

Coordina el flujo:
1. Llama a `JasperFiller.fill()` para llenar el reporte con datos
2. Llama a `ExporterRegistry.getExporter()` para obtener el exportador correcto
3. Llama a `exporter.export()` para convertir a bytes
4. Retorna un `ReportResult`

**No sabe cómo se ejecutan las queries ni cómo se exporta. Solo orquesta.**

### Capa 3 — Servicios especializados

| Clase             | Responsabilidad única                                       |
|-------------------|-------------------------------------------------------------|
| `JasperFiller`    | Compilar `.jrxml` → `.jasper` y llenar reportes con datos   |
| `QueryExecutor`   | Ejecutar SQL y devolver resultados como `List<Map>`         |
| `ExporterRegistry`| Mantener un mapa de formatos → exportadores                 |

### Capa 4 — Exportadores (Strategy Pattern)

Cada formato de exportación es una clase independiente que implementa la interfaz `ReportExporter`:

```
ReportExporter (interfaz)
├── PdfReportExporter      → format: "PDF"
└── ExcelReportExporter    → format: "XLSX"
```

Spring los detecta automáticamente por la anotación `@Component`. El `ExporterRegistry` los recoge en una lista y arma un mapa `formato → exportador`. **No hay if/switch para elegir el formato.**

### Capa 5 — Excepciones

| Excepción                   | Cuándo se lanza                    | HTTP |
|-----------------------------|------------------------------------|------|
| `ReportNotFoundException`   | El archivo `.jasper` no existe     | 404  |
| `InvalidFormatException`    | El formato no es PDF ni XLSX       | 400  |
| `ReportGenerationException` | Error de SQL o de JasperReports    | 500  |

`GlobalExceptionHandler` captura todas y devuelve respuestas HTTP limpias.

---

## 7. Plantillas JRXML y subreportes

Los archivos `.jrxml` son las plantillas de diseño de JasperReports. Al iniciar la aplicación, `JasperFiller` las compila automáticamente a `.jasper` (solo si el `.jrxml` es más reciente).

### Reporte principal: `Laboratorio.jrxml`

Define la estructura general de la página:
- **columnHeader**: logo de la empresa, datos de la empresa, datos del paciente (subreporte)
- **detail**: resultados de Variables (subreporte) + resultados Normales (subreporte)
- **pageFooter**: direcciones y teléfonos de la empresa

### Subreportes

| Archivo          | Parámetro que recibe         | Qué muestra                                    |
|------------------|------------------------------|-------------------------------------------------|
| `Paciente.jrxml` | `DS_ATENCION`                | Nombre, ID, edad, sexo, médico, empresa, fechas |
| `Variables.jrxml`| Datasource directo           | Resultados tipo variable (tabla con bordes)      |
| `Normal.jrxml`   | Datasource directo           | Resultados normales (lista con firma)            |

### Cómo se pasan los datos a los reportes

Cada query se ejecuta y el resultado se empaqueta como `JRMapCollectionDataSource` (una colección de mapas clave-valor). Se inyecta como **parámetro** del reporte principal.

En las expresiones JRXML se accede así:

```java
// Obtener un campo de la primera fila del datasource
$P{DS_ATENCION}.getData().iterator().next().get("nombre")

// Para campos que pueden ser null (como fechas)
($P{DS_ATENCION}.getData().iterator().next().get("FECHA") == null
    ? ""
    : java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
        .format(((java.sql.Date)...).toLocalDate()))
```

---

## 8. Formatos de exportación disponibles

| Formato | Content-Type | Descripción |
|---------|--------------|-------------|
| **PDF** | `application/pdf` | Formato de documento portátil, optimizado para impresión |
| **XLSX** | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` | Hoja de cálculo Excel, datos tabulares |
| **HTML** | `text/html` | Página web con estilos embebidos, visualización en navegador |
| **DOCX** | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` | Documento Word moderno, estructura preserva paginación |

---

## 9. Cómo agregar un nuevo formato de exportación

Para agregar, por ejemplo, exportación a **CSV**:

**1.** Crear una nueva clase en `services/exporters/`:

```java
package com.example.JaspertReport.services.exporters;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class CsvReportExporter implements ReportExporter {

    @Override
    public String getSupportedFormat() { return "CSV"; }

    @Override
    public String getContentType() { return "text/csv"; }

    @Override
    public String getFileExtension() { return "csv"; }

    @Override
    public byte[] export(JasperPrint jasperPrint) throws JRException {
        JRCsvExporter exporter = new JRCsvExporter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleWriterExporterOutput(out));
        exporter.exportReport();
        return out.toByteArray();
    }
}
```

**2.** Listo. No hay paso 2.

Spring detecta la clase por `@Component`, el `ExporterRegistry` la registra automáticamente, y el endpoint ya acepta `"format": "CSV"`. **No se modifica ninguna clase existente.**

---

## 10. Cómo agregar un nuevo reporte

**1.** Diseñar la plantilla `.jrxml` con Jaspersoft Studio u otro editor

**2.** Colocar el archivo `.jrxml` en la carpeta configurada en `app.reportes.ruta`

**3.** Al iniciar la API, el `.jrxml` se compila a `.jasper` automáticamente

**4.** Desde el cliente, enviar la petición con:
- `"reportName"`: el nombre del archivo sin extensión
- `"queries"`: las queries cuyos `param` coincidan con los `<parameter>` del `.jrxml`

**No se modifica ningún archivo Java.**

---

## 11. Configuración

Archivo: `src/main/resources/application.properties`

| Propiedad                              | Descripción                                | Ejemplo                              |
|----------------------------------------|--------------------------------------------|--------------------------------------|
| `service.token`                        | Token de autenticación del servicio        | `mi-token-seguro-2026`              |
| `app.reportes.ruta`                    | Ruta a la carpeta de plantillas `.jrxml`   | `C:/reportes/` (debe terminar en /) |
| `spring.datasource.url`               | URL de conexión JDBC a MySQL               | `jdbc:mysql://localhost:3306/mibd`  |
| `spring.datasource.username`          | Usuario de la BD                           | `root`                               |
| `spring.datasource.password`          | Contraseña de la BD                        | `password123`                        |
| `spring.datasource.hikari.maximum-pool-size` | Máximo de conexiones simultáneas    | `10`                                 |
| `spring.datasource.hikari.minimum-idle`      | Conexiones mínimas abiertas         | `2`                                  |

### Auto-compilación de plantillas

Al arrancar, `JasperFiller` revisa la carpeta `app.reportes.ruta`:
- Si un `.jrxml` no tiene su `.jasper` correspondiente → lo compila
- Si un `.jrxml` es más reciente que su `.jasper` → lo recompila
- Si el `.jasper` está al día → no hace nada

Esto permite actualizar plantillas sin reiniciar la API manualmente (basta con reiniciar el servicio).

---

## 12. Ejemplo completo con Postman

### Configuración

| Campo          | Valor                                        |
|----------------|----------------------------------------------|
| Método         | `POST`                                       |
| URL            | `http://localhost:8080/reportes/generar`      |
| Header         | `X-Service-Token: bajaire-java-service-2026` |
| Header         | `Content-Type: application/json`             |
| Body (raw JSON)| Ver abajo                                    |

### Body

```json
{
  "reportName": "Laboratorio",
  "format": "PDF",
  "queries": [
    {
      "param": "DS_EMPRESA",
      "query": "SELECT * FROM Datos_Empresa"
    },
    {
      "param": "DS_ATENCION",
      "query": "SELECT t1.*, CONCAT(T2.primnomusu, ' ', T2.segunomusu, ' ', T2.primapelli, ' ', T2.seguapelli) AS nombre, t3.Nommed AS Nommed, CONCAT(TRIM(T2.valoredad), ' ', T2.unimededad) AS edadpac, IF(t2.sexo=1, 'Masculino', 'Femenino') AS sexopac, IF(t4.nomemp IS NULL, '', t4.nomemp) AS nomemp, t2.fechanace FROM Mae_Atencion t1 LEFT JOIN mae_pacientes t2 ON t2.NumIdentUs=t1.codpac LEFT JOIN mae_medicos t3 ON t1.codmed=t3.codmed LEFT JOIN mae_empresas t4 ON t1.empresa=t4.codemp WHERE t1.codaten = 'C0011816'"
    },
    {
      "param": "DS_RESULTADOS_VARIABLES",
      "query": "SELECT t1.Codaten, ... FROM Mov_Atencion t1 ... WHERE t1.codaten = 'C0011816' AND t2.tipores = 3 ORDER BY ..."
    },
    {
      "param": "DS_RESULTADOS_NORMAL",
      "query": "SELECT t1.Codaten, ... FROM Mov_Atencion t1 ... WHERE t1.codaten = 'C0011816' AND t2.tipores = 1 ORDER BY ..."
    }
  ]
}
```

### Respuesta esperada

En Postman, cambiar la vista a **Preview** para ver el PDF renderizado directamente, o guardar la respuesta como archivo.

---

## 13. Códigos de respuesta HTTP

| Código | Significado              | Cuándo ocurre                                                |
|--------|--------------------------|--------------------------------------------------------------|
| `200`  | OK                       | Reporte generado correctamente                               |
| `400`  | Bad Request              | Falta `reportName`, `format`, o `queries` vacías/inválidas   |
| `400`  | Bad Request              | Formato no soportado (ni PDF ni XLSX)                        |
| `401`  | Unauthorized             | Token `X-Service-Token` faltante o incorrecto                |
| `404`  | Not Found                | No existe el archivo `.jasper` con ese `reportName`          |
| `500`  | Internal Server Error    | Error de SQL, error al llenar o exportar el reporte          |

---

## 14. Ejemplos de uso para cada formato

### Generar reporte en HTML

```json
{
  "reportName": "Laboratorio",
  "format": "HTML",
  "queries": [
    { "param": "DS_EMPRESA", "query": "SELECT * FROM Datos_Empresa" },
    { "param": "DS_ATENCION", "query": "SELECT ... FROM Mae_Atencion ..." }
  ]
}
```

**Respuesta:** HTML con estilos embebidos, paginación CSS, visualizable en navegador

---

### Generar reporte en DOCX

```json
{
  "reportName": "Laboratorio",
  "format": "DOCX",
  "queries": [ ... ]
}
```

**Respuesta:** Documento Word moderno (Office Open XML) con estructura y paginación preservadas, descargable

---

## 15. Preguntas frecuentes

### "Modifiqué un .jrxml pero no veo los cambios"
Reinicia la API. Al arrancar, `JasperFiller` detecta que el `.jrxml` es más reciente que el `.jasper` y lo recompila automáticamente.

### "Quiero cambiar la base de datos"
Edita `spring.datasource.url`, `username` y `password` en `application.properties` y reinicia.

### "Quiero usar esta API para otro tipo de reporte completamente diferente"
Solo necesitas: diseñar tu `.jrxml`, colocarlo en la carpeta de reportes, y llamar al endpoint con el nombre del nuevo reporte y las queries que necesite. No se modifica ningún código Java.

### "¿Puedo llamar a esta API desde cualquier lenguaje?"
Sí. Solo necesitas hacer un `POST` HTTP con JSON. Funciona desde PHP, Python, JavaScript, C#, o cualquier lenguaje que soporte HTTP.

### "¿Cómo sé qué nombres de `param` usar para un reporte?"
Abre el `.jrxml` del reporte y busca las etiquetas `<parameter name="...">`. Cada `name` es el valor que debes poner en `param` de la query.

### "¿Qué pasa si una query no devuelve resultados?"
Se inyecta un `JRMapCollectionDataSource` vacío. El reporte manejará esa situación según su diseño (puede ocultar secciones con `printWhenExpression`).
