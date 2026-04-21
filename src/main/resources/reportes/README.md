# Reportes / Templates

Esta carpeta contiene las plantillas JRXML y archivos relacionados para generar reportes.

## 📁 Estructura

```
reportes/
├── MiReporte.jrxml          ← Plantillas de reportes
├── MiReporte.jasper         ← Reportes compilados (auto-generado)
├── Subreporte.jrxml         ← Subreportes
├── Logo.jpg                 ← AGREGUE: Su logo/marca aquí
├── Imagen.png               ← AGREGUE: Otras imágenes necesarias
└── .gitkeep                 ← Placeholder para mantener carpeta
```

## 🎨 Agregar Logos e Imágenes

### Paso 1: Copiar archivo
```bash
cp /ruta/a/tu/logo.jpg reportes/Logo.jpg
```

### Paso 2: Usar en JRXML
En JasperStudio, agrega un elemento de imagen:

```xml
<element kind="image" uuid="..." x="0" y="0" width="131" height="73">
    <expression><![CDATA[$P{SUBREPORT_DIR} + "Logo.jpg"]]></expression>
</element>
```

## ⚖️ Consideraciones Legales

**IMPORTANTE:** Los logos e imágenes que agregues aquí son tu responsabilidad legal.

- ✅ Usa solo imágenes/logos que tengas derechos de uso
- ✅ Incluye créditos si corresponde
- ❌ NO uses logos de terceros sin permiso
- ❌ NO uses marcas registradas sin autorización

## 🔒 Seguridad

Las imágenes se ignoran en git para:
- Evitar llenar el repositorio con archivos binarios
- Mantener los logos/imágenes de tu infraestructura locales
- Proteger propiedad intelectual (marcas, logos específicos)

Cada instalación puede tener sus propios logos sin conflictos.

## 📝 Ejemplo: Estructura completa

```
reportes/
├── Laboratorio.jrxml
├── Laboratorio.jasper
├── Paciente.jrxml
├── Paciente.jasper
├── Normal.jrxml
├── Normal.jasper
├── Variables.jrxml
├── Variables.jasper
├── StickerQR.jrxml
├── StickerQR.jasper
├── Logo.jpg               ← Tu logo aquí
├── Watermark.png          ← Marca de agua si quieres
└── .gitkeep
```

---

**Ver:** [DEPLOYMENT.md](../DEPLOYMENT.md) para instrucciones de setup completo.
