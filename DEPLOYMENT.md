# Deployment Guide - JaspertReport

Esta guía te ayudará a instalar y ejecutar JaspertReport en tu máquina.

## 📋 Requisitos Previos

Elige el método de instalación según tu caso:

---

## 🚀 Opción 1: Ejecutable Portable Windows (Más Fácil)

### Requisitos
- **Windows 10+** (64-bit)
- **512 MB RAM** disponible
- **~200 MB espacio en disco**
- **Acceso a MySQL** (BD remota o local)

### Pasos

1. **Descarga el ejecutable portable:**
   - Busca en Releases: `JaspertReport-portable-vX.X.X.zip`
   - O construye uno tu mismo (ver sección "Construir Ejecutable Portable")

2. **Descomprimir:**
   ```
   Descomprime JaspertReport-portable-vX.X.X.zip en una carpeta
   ```

3. **Configurar credenciales:**
   ```
   Abre: JaspertReport-portable-vX.X.X/
   ├── app/
   │   ├── application.properties  ← EDITA ESTE ARCHIVO
   │   └── reportes/              ← Copia tus plantillas JRXML aquí
   ```

   Edita `application.properties`:
   ```properties
   SERVICE_TOKEN=tu-token-secreto
   DB_URL=jdbc:mysql://tu-host:3306/tu-base-datos
   DB_USER=tu-usuario
   DB_PASSWORD=tu-contraseña
   SERVER_PORT=8080
   REPORTES_RUTA=./reportes/
   ```

4. **Ejecutar:**
   - Doble clic en `JaspertReport.exe`
   - O desde PowerShell:
     ```powershell
     .\JaspertReport.exe
     ```

5. **Acceder:**
   ```
   http://localhost:8080/reportes/generar
   ```

### ✅ Ventajas
- ✅ NO requiere instalar Java
- ✅ Totalmente portable (USB, carpeta local, etc.)
- ✅ NO modifica el registro de Windows
- ✅ Fácil desinstalar (solo elimina carpeta)

### ⚠️ Notas
- Si el antivirus marca el `.exe` como sospechoso, es normal (Java empaquetado)
- Agrégalo a excepciones del antivirus si es necesario
- Requiere acceso a la BD especificada en `application.properties`

---

## 💻 Opción 2: JAR Ejecutable (Más Control)

### Requisitos
- **Java 17+** instalado
- **512 MB RAM** disponible
- **~100 MB espacio en disco** (sin runtime)
- **Acceso a MySQL**

### Pasos

1. **Descarga el JAR:**
   - Busca en Releases: `JaspertReport-vX.X.X.jar`
   - O construye uno (ver "Development Guide")

2. **Crea carpeta de trabajo:**
   ```bash
   mkdir JaspertReport
   cd JaspertReport
   ```

3. **Descarga configuración de ejemplo:**
   ```bash
   # Descarga desde: https://github.com/tuusuario/JaspertReport/blob/main/src/main/resources/application.properties.example
   curl -o application.properties https://raw.githubusercontent.com/tuusuario/JaspertReport/main/src/main/resources/application.properties.example
   ```

4. **Edita credenciales:**
   ```bash
   nano application.properties
   # O abre con tu editor favorito
   ```

5. **Crea carpeta de reportes:**
   ```bash
   mkdir reportes
   # Copia tus archivos .jrxml aquí
   ```

6. **Ejecuta:**
   ```bash
   java -jar JaspertReport-vX.X.X.jar
   ```

7. **Accede:**
   ```
   http://localhost:8080/reportes/generar
   ```

---

## 🔧 Solución de Problemas

### Error: "BD no disponible"
```
Solución: Verifica que la BD está corriendo y los parámetros en application.properties son correctos
```

### Error: "Puerto 8080 en uso"
```
Opción 1: Termina el proceso que usa el puerto
Opción 2: Cambia SERVER_PORT en application.properties
   SERVER_PORT=8081
```

### Error: "Reportes no encontrados"
```
Asegúrate que:
1. La carpeta de reportes existe
2. Los archivos .jrxml están ahí
3. REPORTES_RUTA en application.properties apunta al lugar correcto
```

### JasperReport.exe se cierra sin error
```
Solución: Ejecuta desde PowerShell para ver el error:
   .\JaspertReport.exe
```

### "Token inválido" en peticiones
```
Verifica que el header sea exacto:
   X-Service-Token: tu-token-secreto
```

---

## 📊 Estructura de Archivos (Portable)

```
JaspertReport-portable-vX.X.X/
├── JaspertReport.exe          ← Ejecutable
├── runtime/                   ← Java Runtime (no modificar)
├── app/
│   ├── application.properties ← EDITAR: credenciales
│   ├── application.yml
│   ├── JaspertReport-vX.jar   ← Aplicación
│   └── reportes/              ← COPIA AQUÍ: plantillas .jrxml
├── README.txt
└── JaspertReport.ico
```

---

## 🎨 Personalización

### Cambiar Puerto
En `application.properties`:
```properties
SERVER_PORT=9090
```
Luego accede en `http://localhost:9090`

### Cambiar Ruta de Reportes
En `application.properties`:
```properties
# Windows
REPORTES_RUTA=C:/Mis Reportes/

# Linux/Mac
REPORTES_RUTA=/opt/reportes/
```

### Cambiar Token
En `application.properties`:
```properties
SERVICE_TOKEN=mi-token-ultra-secreto-12345
```

---

## 📝 Logs

### Ver logs en tiempo real (JAR)
```bash
java -jar JaspertReport-vX.X.X.jar --logging.level.root=DEBUG
```

### Logs en archivo
```properties
# En application.properties
logging.file.name=logs/jaspertreport.log
logging.file.max-size=10MB
logging.file.max-history=10
```

---

## 🔒 Seguridad en Producción

### Cambiar token por defecto
⚠️ **CRÍTICO:** No uses `dev-token` en producción
```properties
SERVICE_TOKEN=generar-token-fuerte-aqui
```

### SSL/HTTPS
Para usar HTTPS, necesitarás un certificado:
```properties
server.ssl.key-store=/ruta/a/keystore.jks
server.ssl.key-store-password=contraseña
server.ssl.key-store-type=JKS
```

### Firewall
- Abre puerto 8080 (o el que configuraste) solo para IPs autorizadas
- Restringe acceso a la BD a solo la app

### Backups
- Respalda regularmente la carpeta `reportes/`
- Respalda la BD (MySQL)

---

## 📞 Soporte

- **Documentación:** Ver `README.md` en el repositorio
- **Issues:** https://github.com/tuusuario/JaspertReport/issues
- **API Docs:** Ver `README.md` (sección de API REST)
