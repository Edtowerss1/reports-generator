# Guía de Contribución

¡Gracias por considerar contribuir a JaspertReport! Este documento proporciona pautas y direcciones para contribuir al proyecto.

## 📋 Código de Conducta

Por favor, sé respetuoso con otros colaboradores. El acoso, la discriminación o cualquier comportamiento negativo no será tolerado.

## 🐛 ¿Encontraste un Bug?

1. **Verifica** que el bug no haya sido reportado en [Issues](https://github.com/Edtowerss1/reports-generator/issues)
2. **Abre una Issue** con el siguiente formato:

```markdown
**Descripción del problema**
Describe claramente qué no funciona.

**Pasos para reproducir**
1. ...
2. ...
3. ...

**Comportamiento esperado**
Qué deberían hacer.

**Comportamiento actual**
Qué está pasando.

**Entorno**
- Java version: x.x.x
- Spring Boot version: x.x.x
- Sistema operativo: Windows/Linux/Mac
- Base de datos: MySQL x.x.x
```

## 🎯 ¿Tienes una idea de mejora?

1. Usa [GitHub Discussions](https://github.com/Edtowerss1/reports-generator/discussions) para proponer ideas
2. Describe claramente:
   - Qué problema resuelve
   - Casos de uso
   - Posibles alternativas
   - Ejemplos de código (si aplica)

## 💻 Contribuir Código

### Configuración de Desarrollo

```bash
# Clonar el repositorio
git clone https://github.com/tuusuario/JaspertReport.git
cd JaspertReport

# Crear rama de feature
git checkout -b feature/mi-feature

# Instalar dependencias y compilar
mvn clean install
```

### Antes de hacer Push

1. **Cumple con el código existente:**
   - Sigue las convenciones de nomenclatura del proyecto
   - Usa el mismo estilo de indentación (2 espacios)
   - Documenta métodos complejos

2. **Pruebas:**
   ```bash
   mvn test
   ```

3. **Commits claros:**
   - Usa commits semánticos: `feat:`, `fix:`, `docs:`, `refactor:`, etc.
   - Ejemplo: `feat: add CSV exporter` o `fix: null pointer in QueryExecutor`

4. **Rebase antes de PR:**
   ```bash
   git fetch origin
   git rebase origin/main
   ```

### Proceso de Pull Request

1. **Push a tu fork:**
   ```bash
   git push origin feature/mi-feature
   ```

2. **Abre un Pull Request** con:
   - Descripción clara del cambio
   - Enlace a Issues relacionadas (si existen)
   - Cambios principales
   - Pruebas incluidas

3. **Responde a comentarios de revisor** con paciencia

### Estructura de Directorios

```
src/
├── main/
│   ├── java/com/example/JaspertReport/
│   │   ├── controllers/        ← REST endpoints
│   │   ├── services/          ← Lógica de negocio
│   │   ├── dtos/              ← Data Transfer Objects
│   │   └── exceptions/        ← Excepciones personalizadas
│   └── resources/
│       ├── application.properties
│       └── reportes/          ← Plantillas JRXML
└── test/
    └── java/com/example/JaspertReport/
```

## 🏗️ Principios Arquitectónicos

El proyecto sigue estos principios:

- **SOLID**: Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion
- **Clean Architecture**: Separación clara entre capas
- **Strategy Pattern**: Extensibilidad de exporters sin modificar código existente

## 📖 Documentación

Si agregas una feature:

1. Actualiza el `README.md`
2. Agrega comentarios en el código si es complejo
3. Crea/actualiza documentación en `docs/` si es necesario

## 🚀 Release Notes

Cambios importantes van a `CHANGELOG.md` (si existe).

## ❓ ¿Preguntas?

- Abre una [Discussion](https://github.com/Edtowerss1/reports-generator/discussions)
- Revisa la documentación en `docs/`

---

**¡Gracias por tu contribución!** 🙌
