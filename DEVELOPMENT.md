# Development Guide - JaspertReport

Guía para desarrolladores que quieran compilar desde source, contribuir o customizar.

---

## 🔧 Requisitos para Desarrollo

- **Java 17+** (JDK, no JRE)
- **Maven 3.8+** (o usar `mvnw` incluido)
- **MySQL 8.0+** (para probar con BD real)
- **Git**
- **Un IDE** (IntelliJ IDEA, Eclipse, VS Code con Java plugins)

---

## 📥 Setup Local

### 1. Clona el repositorio

```bash
git clone https://github.com/tuusuario/JaspertReport.git
cd JaspertReport
```

### 2. Configura la BD

```bash
# Opción A: MySQL local
mysql -u root -p
CREATE DATABASE jaspertreport_dev;
USE jaspertreport_dev;
# Importa tu schema aquí si tienes uno
exit
```

O usa Docker:

```bash
docker run -d \
  --name jaspertreport-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=jaspertreport_dev \
  -p 3306:3306 \
  mysql:8.0
```

### 3. Crea archivo de configuración

```bash
cp src/main/resources/application.properties.example \
   src/main/resources/application.properties
```

Edita `src/main/resources/application.properties`:

```properties
SERVICE_TOKEN=dev-token-123
DB_URL=jdbc:mysql://localhost:3306/jaspertreport_dev
DB_USER=root
DB_PASSWORD=root
REPORTES_RUTA=/ruta/local/a/reportes/
SERVER_PORT=8080
```

### 4. Compila el proyecto

```bash
# Con Maven instalado
mvn clean install

# O con Maven wrapper (Windows)
mvnw.cmd clean install

# O con Maven wrapper (Linux/Mac)
./mvnw clean install
```

### 5. Ejecuta la aplicación

```bash
# Opción A: Desde Maven
mvn spring-boot:run

# Opción B: Desde JAR compilado
java -jar target/JaspertReport-0.0.1-SNAPSHOT.jar

# Opción C: Desde IDE
Clic derecho en JaspertReportApplication.java → Run
```

### 6. Verifica que está corriendo

```bash
# Debería retornar 401 (sin token válido)
curl -X POST http://localhost:8080/reportes/generar \
  -H "Content-Type: application/json" \
  -d '{"reportName":"test"}'

# Con token válido
curl -X POST http://localhost:8080/reportes/generar \
  -H "X-Service-Token: dev-token-123" \
  -H "Content-Type: application/json" \
  -d '{
    "reportName":"MiReporte",
    "format":"PDF",
    "queries":[{"param":"DS_DATOS","query":"SELECT 1"}]
  }'
```

---

## 🏗️ Estructura del Proyecto

```
JaspertReport/
├── src/
│   ├── main/
│   │   ├── java/com/example/JaspertReport/
│   │   │   ├── controllers/          ← REST endpoints
│   │   │   ├── services/             ← Lógica de negocio
│   │   │   │   ├── ReportOrchestrator.java
│   │   │   │   ├── JasperFiller.java
│   │   │   │   ├── QueryExecutor.java
│   │   │   │   ├── ReportPrintService.java
│   │   │   │   └── exporters/        ← Strategy pattern
│   │   │   ├── dtos/                 ← Data Transfer Objects
│   │   │   └── exceptions/           ← Error handling
│   │   └── resources/
│   │       ├── application.properties (gitignored)
│   │       ├── application.properties.example
│   │       ├── reportes/             ← JRXML templates
│   │       └── fonts/                ← Custom fonts
│   └── test/
│       └── java/com/example/JaspertReport/
├── pom.xml                           ← Maven config
├── mvnw / mvnw.cmd                   ← Maven wrapper
├── README.md                         ← Para usuarios
├── DEPLOYMENT.md                     ← Para instalar
└── CONTRIBUTING.md                   ← Para colaboradores
```

---

## 🧪 Tests

### Ejecutar todos los tests

```bash
mvn test
```

### Ejecutar un test específico

```bash
mvn test -Dtest=ReportControllerTest
```

### Ver cobertura

```bash
mvn test jacoco:report
# Abre: target/site/jacoco/index.html
```

### Tests integración

```bash
mvn verify
```

---

## 🎨 Agregar un Nuevo Exporter

El proyecto usa **Strategy Pattern** para exporters.

### Paso 1: Crear clase

```java
// src/main/java/com/example/JaspertReport/services/exporters/CsvReportExporter.java

package com.example.JaspertReport.services.exporters;

import net.sf.jasperreports.engine.JRException;
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
        // TODO: Implementar lógica de exportación a CSV
        return new byte[0];
    }
}
```

### Paso 2: Implementar lógica

```java
@Override
public byte[] export(JasperPrint jasperPrint) throws JRException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    // Usar JasperReports para exportar a CSV
    JRCsvExporter exporter = new JRCsvExporter();
    exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
    exporter.setExporterOutput(new SimpleWriterExporterOutput(baos));
    exporter.exportReport();
    
    return baos.toByteArray();
}
```

### Paso 3: ¡Listo!

El `ExporterRegistry` lo detectará automáticamente vía `@Component` y inyección de lista.

Prueba con:
```bash
curl -X POST http://localhost:8080/reportes/generar \
  -H "X-Service-Token: dev-token-123" \
  -H "Content-Type: application/json" \
  -d '{
    "reportName":"MiReporte",
    "format":"CSV",
    "queries":[...]
  }'
```

---

## 📝 Commits Semánticos

Por favor, usa commits convencionales:

```bash
# Feature
git commit -m "feat: add CSV exporter"

# Bug fix
git commit -m "fix: null pointer in QueryExecutor"

# Documentation
git commit -m "docs: update API reference"

# Refactoring
git commit -m "refactor: simplify ReportOrchestrator logic"

# Tests
git commit -m "test: add tests for ReportExporter"
```

---

## 🔄 Workflow Git

1. **Crea rama de feature:**
   ```bash
   git checkout -b feature/tu-feature
   ```

2. **Haz cambios y commits:**
   ```bash
   git add .
   git commit -m "feat: descripcción de tu feature"
   ```

3. **Push a tu fork:**
   ```bash
   git push origin feature/tu-feature
   ```

4. **Abre Pull Request en GitHub**

---

## 🐛 Debugging

### Con IDE (recomendado)

1. Abre proyecto en IntelliJ IDEA / Eclipse
2. Haz clic en línea donde quieres breakpoint
3. Click derecho → Debug 'JaspertReportApplication'

### Con logs

Edita `src/main/resources/application.properties`:

```properties
# Debug mode
logging.level.com.example.JaspertReport=DEBUG
logging.level.org.springframework.boot=DEBUG

# Log a archivo
logging.file.name=logs/debug.log
```

### Remote debugging

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
  -jar target/JaspertReport-0.0.1-SNAPSHOT.jar
```

Luego en IDE: Run → Edit Configurations → + Debug (remote)

---

## 📦 Construir Ejecutable Portable (Windows)

### Prerequisitos
- **jpackage** (incluido en Java 17+)
- **WiX Toolset** instalado (para .msi)

### Pasos

1. **Compila el JAR:**
   ```bash
   mvn clean package
   ```

2. **Crea ejecutable:**
   ```powershell
   $JAVA_HOME = "C:\Program Files\Java\jdk-17"  # Ajusta según tu JDK
   
   & "$JAVA_HOME\bin\jpackage.exe" `
     --input target `
     --name JaspertReport `
     --main-jar JaspertReport-0.0.1-SNAPSHOT.jar `
     --main-class com.example.JaspertReport.JaspertReportApplication `
     --type exe `
     --app-version 1.0.0 `
     --vendor "TuEmpresa" `
     --win-console `
     --dest target/jpackage
   ```

3. **Resultado:**
   ```
   target/jpackage/JaspertReport-1.0.0.exe
   ```

---

## 📚 Recursos

- **Spring Boot Docs:** https://spring.io/projects/spring-boot
- **JasperReports API:** https://jasperreports.sourceforge.net/
- **Maven Guide:** https://maven.apache.org/guides/
- **API Reference:** Ver sección "API REST" en `README.md`

---

## 🆘 Problemas Comunes

### "Cannot find a matching compiler"
```
Solución: Instala JDK 17+ (no JRE)
Verifica: java -version
```

### "Port 8080 already in use"
```
Solución: 
  Opción 1: Cambia SERVER_PORT en application.properties
  Opción 2: Mata el proceso en el puerto (lsof -i :8080)
```

### "BD connection refused"
```
Solución:
  1. Verifica que MySQL está corriendo
  2. Verifica credenciales en application.properties
  3. mysql -u root -p -h localhost (prueba conexión)
```

---

**¿Algo no funciona? Abre una Issue en GitHub!**
