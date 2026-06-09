# Example Report Templates

These are **example templates** for the JasperReport multi-tenant engine.
They demonstrate the two main report patterns:

| Template | Type | Description |
|----------|------|-------------|
| `SimpleReport.jrxml` | Single datasource | Basic tabular report with header, detail rows, and page footer |
| `MasterReport.jrxml` | Master with subreport | Report with company header (`DS_EMPRESA`) and a subreport call to `DetailReport.jasper` |
| `DetailReport.jrxml` | Subreport | Detail lines with ID, name, quantity, price, and calculated total |

## Parameter Conventions

- `DS_*` — `JRDataSource` parameters fed by SQL query results from the engine
- `DS_EMPRESA` — `JRMapCollectionDataSource` for single-row company/header info
- `SUBREPORT_DIR` — `String` parameter set automatically per tenant by the engine

## Usage

Place `.jrxml` files in the tenant's configured `reportes-ruta` directory
(`app.tenants.<id>.reportes-ruta` in `application.properties`).

The engine compiles them to `.jasper` automatically on first request (lazy compilation
via `LazyReportCompiler`).

**Important:**
- **No shared fallback** — if a template is not found in the tenant's directory,
  the request fails with 404. There is no global template directory.
- **SUBREPORT_DIR is tenant-scoped** — the engine sets it to the tenant's
  `reportes-ruta` automatically. Subreports must be in the same tenant directory.
- **Pre-compiled `.jasper` files** are shipped with the project for the example
  templates. The lazy compiler skips recompilation if the `.jasper` is up to date.
- **Field types must match** — JRXML fields are `java.lang.String`. SQL queries
  must return VARCHAR columns. Numeric columns will not be auto-cast (null values).
